import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { CartService } from '../../services/cart.service';
import { AuthService } from '../../services/auth.service';
import { CheckoutModalComponent } from '../checkout-modal/checkout-modal.component';
import { AuthModalComponent } from '../auth-modal/auth-modal.component';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterModule, CheckoutModalComponent, AuthModalComponent],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css']
})
export class CartComponent {
  readonly cart = inject(CartService);
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  isCheckoutOpen = signal(false);
  isAuthModalOpen = signal(false);

  openCheckout(): void {
    if (!this.auth.isAuthenticated()) {
      this.isAuthModalOpen.set(true);
      return;
    }
    this.isCheckoutOpen.set(true);
  }

  closeCheckout(): void {
    this.isCheckoutOpen.set(false);
  }

  onOrderSuccess(orderId: string): void {
    this.cart.clearCart();
    this.isCheckoutOpen.set(false);
    this.router.navigate(['/orders']);
  }
}
