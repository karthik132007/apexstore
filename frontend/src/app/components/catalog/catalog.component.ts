import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { RevealDirective } from '../../directives/reveal.directive';
import { ProductService } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { Product } from '../../models/ecom.models';
import { BannerStripComponent } from '../banner-strip/banner-strip.component';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, BannerStripComponent, RevealDirective],
  templateUrl: './catalog.component.html',
  styleUrls: ['./catalog.component.css']
})
export class CatalogComponent implements OnInit, OnDestroy {
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private querySubscription?: Subscription;
  private productSubscription?: Subscription;

  ngOnDestroy(): void {
    this.querySubscription?.unsubscribe();
    this.productSubscription?.unsubscribe();
  }

  products = signal<Product[]>([]);
  isLoading = signal(true);
  loadError = signal(false);
  filtersOpen = signal(false);

  // Pagination & meta
  currentPage = signal(0);
  pageSize = signal(12);
  totalElements = signal(0);
  totalPages = signal(1);

  // Filter state
  activeCategory = signal<string>('All');
  searchKeyword = signal<string>('');
  selectedSort = signal<string>('createdAt,desc');

  // Client-side visual filters
  minPrice = signal<number | null>(null);
  maxPrice = signal<number | null>(null);
  inStockOnly = signal<boolean>(false);

  // Wishlist set
  wishlist = signal<Set<string>>(new Set());

  readonly categoriesList = [
    'All',
    'Electronics',
    'Fashion',
    'Appliances',
    'Home',
    'Books'
  ];

  readonly sortOptions = [
    { label: 'Price: low to high', value: 'price,asc' },
    { label: 'Price: high to low', value: 'price,desc' },
    { label: 'Newest First', value: 'createdAt,desc' }
  ];

  ngOnInit(): void {
    this.querySubscription = this.route.queryParams.subscribe(params => {
      const cat = params['category'] || 'All';
      const search = params['search'] || '';
      const sort = params['sort'] || 'createdAt,desc';
      const page = params['page'] ? +params['page'] : 0;

      this.activeCategory.set(cat);
      this.searchKeyword.set(search);
      this.selectedSort.set(sort);
      this.currentPage.set(page);

      this.loadProducts();
    });
  }

  loadProducts(): void {
    this.isLoading.set(true);
    this.loadError.set(false);

    this.productSubscription?.unsubscribe();
    this.productSubscription = this.productService.getProducts({
      page: this.currentPage(),
      size: this.pageSize(),
      sort: this.selectedSort(),
      search: this.searchKeyword() || undefined,
      category: this.activeCategory() !== 'All' ? this.activeCategory() : undefined
    }).subscribe({
      next: (res) => {
        this.products.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.loadError.set(true);
        this.products.set([]);
        this.totalElements.set(0);
        this.totalPages.set(0);
      }
    });
  }

  filteredProducts(): Product[] {
    let list = this.products();

    if (this.minPrice() !== null) {
      list = list.filter(p => p.price >= this.minPrice()!);
    }
    if (this.maxPrice() !== null) {
      list = list.filter(p => p.price <= this.maxPrice()!);
    }

    if (this.inStockOnly()) {
      list = list.filter(p => p.availableStock > 0);
    }

    return list;
  }

  onSortChange(sortValue: string): void {
    this.selectedSort.set(sortValue);
    this.updateQueryParams({ sort: sortValue, page: 0 });
  }

  onCategorySelect(cat: string): void {
    this.activeCategory.set(cat);
    this.updateQueryParams({ category: cat !== 'All' ? cat : null, page: 0 });
  }

  setPriceRange(min: number | null, max: number | null): void {
    this.minPrice.set(min);
    this.maxPrice.set(max);
  }


  clearFilters(): void {
    this.minPrice.set(null);
    this.maxPrice.set(null);
    this.inStockOnly.set(false);
    this.activeCategory.set('All');
    this.searchKeyword.set('');
    this.selectedSort.set('createdAt,desc');
    this.router.navigate(['/'], { fragment: 'products' });
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.updateQueryParams({ page });
      document.getElementById('products')?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' });
    }
  }

  private updateQueryParams(params: Record<string, any>): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge',
      fragment: 'products'
    });
  }

  addToCart(product: Product, event: MouseEvent): void {
    event.stopPropagation();
    this.cartService.addToCart(product, 1);
  }

  buyNow(product: Product, event: MouseEvent): void {
    event.stopPropagation();
    this.cartService.addToCart(product, 1);
    this.router.navigate(['/cart']);
  }

  toggleWishlist(productId: string, event: MouseEvent): void {
    event.stopPropagation();
    this.wishlist.update(set => {
      const next = new Set(set);
      if (next.has(productId)) {
        next.delete(productId);
        this.toast.info('Removed from your Wishlist.');
      } else {
        next.add(productId);
        this.toast.success('Added to your Wishlist!');
      }
      return next;
    });
  }

  onImageError(event: Event): void {
    const image = event.target as HTMLImageElement;
    image.onerror = null;
    image.src = '/product-placeholder.svg';
  }

  isWishlisted(productId: string): boolean {
    return this.wishlist().has(productId);
  }
}
