import { Routes } from '@angular/router';
import { CatalogComponent } from './components/catalog/catalog.component';
import { ProductDetailComponent } from './components/product-detail/product-detail.component';
import { CartComponent } from './components/cart/cart.component';
import { OrdersComponent } from './components/orders/orders.component';
import { SellerAdminComponent } from './components/seller-admin/seller-admin.component';

export const routes: Routes = [
  { path: '', component: CatalogComponent, pathMatch: 'full' },
  { path: 'product/:id', component: ProductDetailComponent },
  { path: 'cart', component: CartComponent },
  { path: 'orders', component: OrdersComponent },
  { path: 'seller', component: SellerAdminComponent },
  { path: 'admin', component: SellerAdminComponent },
  { path: '**', redirectTo: '' }
];
