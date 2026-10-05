import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ProductService } from '../../services/product.service';
import { ToastService } from '../../services/toast.service';
import { Product, ProductCreatePayload } from '../../models/ecom.models';
import { AuthModalComponent } from '../auth-modal/auth-modal.component';

@Component({
  selector: 'app-seller-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AuthModalComponent],
  templateUrl: './seller-admin.component.html',
  styleUrls: ['./seller-admin.component.css']
})
export class SellerAdminComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly productService = inject(ProductService);
  private readonly toast = inject(ToastService);

  products = signal<Product[]>([]);
  isLoading = signal(true);
  activeTab = signal<'LISTINGS' | 'ADD_PRODUCT'>('LISTINGS');
  isAuthModalOpen = signal(false);

  // New product form
  newSku = '';
  newName = '';
  newDescription = '';
  newCategory = 'Electronics';
  newPrice = 999;
  newImageUrl = '';
  newInitialStock = 25;
  isCreating = signal(false);

  // Stock edit modal
  editingProduct = signal<Product | null>(null);
  stockToUpdate = 0;
  isUpdatingStock = signal(false);

  // Edit details modal
  editModalProduct = signal<Product | null>(null);

  readonly categoryOptions = [
    'Electronics',
    'Fashion',
    'Appliances',
    'Home',
    'Books'
  ];

  readonly sampleImages = [
    { label: 'Mechanical Keyboard', url: 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&auto=format&fit=crop&q=80' },
    { label: 'Wireless Headphones', url: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80' },
    { label: 'Smart Watch', url: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80' },
    { label: 'Gaming Mouse', url: 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=600&auto=format&fit=crop&q=80' },
    { label: 'Running Shoes', url: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&auto=format&fit=crop&q=80' }
  ];

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      if (this.auth.isSeller() || this.auth.isAdmin()) {
        this.loadProducts();
      } else {
        this.isLoading.set(false);
      }
    } else {
      this.isLoading.set(false);
    }
  }

  loadProducts(): void {
    this.isLoading.set(true);
    const req = this.auth.isAdmin() 
      ? this.productService.getAdminProducts() 
      : this.productService.getSellerProducts();

    req.subscribe({
      next: (list) => {
        this.products.set(list);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.toast.error('Failed to load listings: ' + (err.error?.message || err.message));
      }
    });
  }

  becomeSeller(): void {
    this.auth.becomeSeller().subscribe({
      next: () => {
        this.loadProducts();
      }
    });
  }

  openStockModal(p: Product): void {
    this.editingProduct.set(p);
    this.stockToUpdate = p.stockOnHand;
  }

  saveStock(): void {
    const p = this.editingProduct();
    if (!p) return;

    if (this.stockToUpdate < p.reserved) {
      this.toast.error(`Stock cannot be set lower than active reservations (${p.reserved}).`);
      return;
    }

    this.isUpdatingStock.set(true);
    const req = this.auth.isAdmin()
      ? this.productService.updateAdminStock(p.id, this.stockToUpdate)
      : this.productService.updateSellerStock(p.id, this.stockToUpdate);

    req.subscribe({
      next: (updated) => {
        this.isUpdatingStock.set(false);
        this.toast.success(`Inventory for "${p.name}" updated to ${updated.stockOnHand} units.`);
        this.editingProduct.set(null);
        this.loadProducts();
      },
      error: (err) => {
        this.isUpdatingStock.set(false);
        this.toast.error('Failed to update stock: ' + (err.error?.message || err.message));
      }
    });
  }

  openEditModal(p: Product): void {
    this.editModalProduct.set({ ...p });
  }

  saveProductDetails(): void {
    const p = this.editModalProduct();
    if (!p) return;

    const payload: ProductCreatePayload = {
      sku: p.sku,
      name: p.name,
      description: p.description,
      category: p.category,
      price: p.price,
      imageUrl: p.imageUrl
    };

    const req = this.auth.isAdmin()
      ? this.productService.updateAdminProduct(p.id, payload)
      : this.productService.updateSellerProduct(p.id, payload);

    req.subscribe({
      next: () => {
        this.toast.success(`Listing "${p.name}" details updated.`);
        this.editModalProduct.set(null);
        this.loadProducts();
      },
      error: (err) => {
        this.toast.error('Failed to update listing: ' + (err.error?.message || err.message));
      }
    });
  }

  deactivateProduct(p: Product): void {
    if (!confirm(`Are you sure you want to deactivate "${p.name}"? It will no longer appear in public search.`)) {
      return;
    }

    const req = this.auth.isAdmin()
      ? this.productService.deleteAdminProduct(p.id)
      : this.productService.deleteSellerProduct(p.id);

    req.subscribe({
      next: () => {
        this.toast.success(`Product "${p.name}" was deactivated.`);
        this.loadProducts();
      },
      error: (err) => {
        this.toast.error('Failed to deactivate listing: ' + (err.error?.message || err.message));
      }
    });
  }

  createProduct(): void {
    if (!this.newSku || !this.newName || !this.newDescription || this.newPrice <= 0) {
      this.toast.error('Please complete all product fields with valid values.');
      return;
    }

    this.isCreating.set(true);

    const payload: ProductCreatePayload = {
      sku: this.newSku.trim(),
      name: this.newName.trim(),
      description: this.newDescription.trim(),
      category: this.newCategory,
      price: +this.newPrice,
      imageUrl: this.newImageUrl.trim()
    };

    const createReq = this.auth.isAdmin()
      ? this.productService.createAdminProduct(payload)
      : this.productService.createSellerProduct(payload);

    createReq.subscribe({
      next: (created) => {
        // Backend creates products with 0 stock by default; immediately update stock if requested
        if (this.newInitialStock > 0) {
          const stockReq = this.auth.isAdmin()
            ? this.productService.updateAdminStock(created.id, this.newInitialStock)
            : this.productService.updateSellerStock(created.id, this.newInitialStock);

          stockReq.subscribe({
            next: () => {
              this.isCreating.set(false);
              this.toast.success(`Product "${created.name}" created with ${this.newInitialStock} units in stock!`);
              this.resetForm();
              this.activeTab.set('LISTINGS');
              this.loadProducts();
            },
            error: () => {
              this.isCreating.set(false);
              this.toast.warning(`Product created, but stock update failed. Please update stock in listings.`);
              this.resetForm();
              this.activeTab.set('LISTINGS');
              this.loadProducts();
            }
          });
        } else {
          this.isCreating.set(false);
          this.toast.success(`Product "${created.name}" created (0 stock).`);
          this.resetForm();
          this.activeTab.set('LISTINGS');
          this.loadProducts();
        }
      },
      error: (err) => {
        this.isCreating.set(false);
        this.toast.error('Failed to create listing: ' + (err.error?.message || err.message));
      }
    });
  }

  resetForm(): void {
    this.newSku = '';
    this.newName = '';
    this.newDescription = '';
    this.newPrice = 999;
    this.newInitialStock = 25;
  }
}
