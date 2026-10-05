import { Component, EventEmitter, OnInit, OnDestroy, Output, inject, signal } from '@angular/core';
import { DialogDirective } from '../../directives/dialog.directive';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { Order, OrderRequest, OrderStatus } from '../../models/ecom.models';

@Component({
  selector: 'app-checkout-modal',
  standalone: true,
  imports: [DialogDirective, CommonModule, FormsModule],
  templateUrl: './checkout-modal.component.html',
  styleUrls: ['./checkout-modal.component.css']
})
export class CheckoutModalComponent implements OnInit, OnDestroy {
  private readonly auth = inject(AuthService);
  readonly cart = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly toast = inject(ToastService);

  @Output() close = new EventEmitter<void>();
  @Output() orderCompleted = new EventEmitter<string>();

  // Checkout inputs
  customerName = '';
  customerEmail = '';
  address = '';
  private pollingTimer?: ReturnType<typeof setInterval>;

  // Order state tracking
  isSubmitting = signal(false);
  isPolling = signal(false);
  pollCount = signal(0);
  errorMessage = signal<string | null>(null);
  placedOrder = signal<Order | null>(null);
  idempotencyKey = signal('');

  ngOnInit(): void {
    const user = this.auth.currentUser();
    if (user) {
      this.customerName = user.name || '';
      this.customerEmail = user.email || '';
    } else {
      this.customerName = '';
      this.customerEmail = '';
    }
    this.generateNewIdempotencyKey();
  }

  generateNewIdempotencyKey(): void {
    const rand = Math.random().toString(36).substring(2, 9);
    this.idempotencyKey.set(`chk-${Date.now()}-${rand}`);
  }

  submitOrder(): void {
    if (this.isSubmitting()) return;
    this.errorMessage.set(null);

    if (!this.customerName.trim() || !this.customerEmail.trim() || !this.address.trim()) {
      this.errorMessage.set('Please fill out all contact and address fields.');
      return;
    }

    const cartItems = this.cart.items();
    if (cartItems.length === 0) {
      this.errorMessage.set('Your cart is empty.');
      return;
    }

    const payload: OrderRequest = {
      customerName: this.customerName.trim(),
      customerEmail: this.customerEmail.trim(),
      address: this.address.trim(),
      items: cartItems.map(i => ({
        productId: i.product.id,
        quantity: i.quantity
      }))
    };

    this.isSubmitting.set(true);

    this.orderService.placeOrder(payload, this.idempotencyKey()).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        const order = response.body;

        if (response.status === 200 && order) {
          // Completed immediately
          this.placedOrder.set(order);
          this.toast.success(`Order #${order.id.substring(0, 8)} successfully placed!`);
        } else if (response.status === 202 && order) {
          // 202 PENDING - background recovery
          this.placedOrder.set(order);
          this.startPolling(order.id);
        } else if (order) {
          this.placedOrder.set(order);
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        if (err.status === 409 && err.error?.id) {
          this.placedOrder.set(err.error);
          this.errorMessage.set('One or more items are no longer available in the requested quantity.');
        } else {
          this.errorMessage.set(err.error?.message || 'Failed to place order. Check stock availability.');
        }
      }
    });
  }

  private startPolling(orderId: string): void {
    this.isPolling.set(true);
    this.pollCount.set(0);

    this.pollingTimer = setInterval(() => {
      this.pollCount.update(c => c + 1);

      this.orderService.getOrder(orderId).subscribe({
        next: (order) => {
          this.placedOrder.set(order);
          if (order.status === 'CONFIRMED') {
            clearInterval(this.pollingTimer);
            this.isPolling.set(false);
            this.toast.success(`Order #${order.id.substring(0, 8)} confirmed!`);
          } else if (order.status === 'FAILED') {
            clearInterval(this.pollingTimer);
            this.isPolling.set(false);
            this.errorMessage.set('One or more items are no longer available. Please review your cart.');
          } else if (this.pollCount() > 15) {
            clearInterval(this.pollingTimer);
            this.isPolling.set(false);
            this.errorMessage.set('Your order is still being confirmed. You can check its progress in My Orders.');
          }
        },
        error: () => {
          if (this.pollCount() > 15) {
            clearInterval(this.pollingTimer);
            this.isPolling.set(false);
          }
        }
      });
    }, 2000);
  }

  ngOnDestroy(): void { clearInterval(this.pollingTimer); }

  finishOrder(): void {
    const order = this.placedOrder();
    if (order) {
      this.orderCompleted.emit(order.id);
    } else {
      this.close.emit();
    }
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      if (!this.isSubmitting() && !this.isPolling()) {
        this.close.emit();
      }
    }
  }
}
