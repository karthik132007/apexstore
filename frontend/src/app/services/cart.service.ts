import { Injectable, computed, inject, signal } from '@angular/core';
import { CartItem, Product } from '../models/ecom.models';
import { ToastService } from './toast.service';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly toast = inject(ToastService);
  private readonly itemsSignal = signal<CartItem[]>(this.loadCart());

  readonly items = this.itemsSignal.asReadonly();

  readonly totalCount = computed(() => {
    return this.itemsSignal().reduce((sum, item) => sum + item.quantity, 0);
  });

  readonly subtotal = computed(() => {
    return this.itemsSignal().reduce((sum, item) => sum + item.product.price * item.quantity, 0);
  });

  readonly mrpTotal = computed(() => {
    return this.itemsSignal().reduce((sum, item) => {
      const orig = item.product.price;
      return sum + orig * item.quantity;
    }, 0);
  });

  readonly savingsTotal = computed(() => {
    return Math.max(0, this.mrpTotal() - this.subtotal());
  });

  private loadCart(): CartItem[] {
    try {
      const data = localStorage.getItem('ecom_cart');
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  }

  private saveCart(items: CartItem[]): void {
    this.itemsSignal.set(items);
    localStorage.setItem('ecom_cart', JSON.stringify(items));
  }

  addToCart(product: Product, quantity = 1): void {
    const current = this.itemsSignal();
    const existingIndex = current.findIndex(i => i.product.id === product.id);

    if (existingIndex > -1) {
      const updated = [...current];
      const maxAvailable = product.availableStock;
      const newQty = updated[existingIndex].quantity + quantity;

      if (maxAvailable > 0 && newQty > maxAvailable) {
        this.toast.warning(`Only ${maxAvailable} unit(s) available in stock.`);
        updated[existingIndex].quantity = maxAvailable;
      } else {
        updated[existingIndex].quantity = newQty;
        this.toast.success(`Updated quantity of "${product.name}" in your cart.`);
      }
      this.saveCart(updated);
    } else {
      if (product.availableStock <= 0) {
        this.toast.error('This product is currently out of stock.');
        return;
      }
      const initialQty = Math.min(quantity, product.availableStock);
      this.saveCart([...current, { product, quantity: initialQty }]);
      this.toast.success(`Added "${product.name}" to your cart!`);
    }
  }

  updateQuantity(productId: string, quantity: number): void {
    if (quantity <= 0) {
      this.removeFromCart(productId);
      return;
    }

    const current = this.itemsSignal();
    const updated = current.map(item => {
      if (item.product.id === productId) {
        const capped = item.product.availableStock > 0 ? Math.min(quantity, item.product.availableStock) : quantity;
        return { ...item, quantity: capped };
      }
      return item;
    });

    this.saveCart(updated);
  }

  removeFromCart(productId: string): void {
    const current = this.itemsSignal();
    const item = current.find(i => i.product.id === productId);
    this.saveCart(current.filter(i => i.product.id !== productId));
    if (item) {
      this.toast.info(`Removed "${item.product.name}" from your cart.`);
    }
  }

  clearCart(): void {
    this.saveCart([]);
  }
}
