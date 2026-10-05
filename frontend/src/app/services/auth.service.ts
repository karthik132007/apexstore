import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_ENDPOINTS } from '../config/api.config';
import { AuthResponse, User } from '../models/ecom.models';
import { ToastService } from './toast.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);

  private readonly tokenSignal = signal<string | null>(localStorage.getItem('ecom_token'));
  private readonly userSignal = signal<User | null>(this.loadStoredUser());

  readonly token = this.tokenSignal.asReadonly();
  readonly currentUser = this.userSignal.asReadonly();

  readonly isAuthenticated = computed(() => !!this.tokenSignal() && !!this.userSignal());
  readonly isSeller = computed(() => this.userSignal()?.roles?.includes('SELLER') ?? false);
  readonly isAdmin = computed(() => this.userSignal()?.roles?.includes('ADMIN') ?? false);

  private loadStoredUser(): User | null {
    try {
      const raw = localStorage.getItem('ecom_user');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  setSession(auth: AuthResponse): void {
    this.tokenSignal.set(auth.accessToken);
    this.userSignal.set(auth.user);
    localStorage.setItem('ecom_token', auth.accessToken);
    localStorage.setItem('ecom_user', JSON.stringify(auth.user));
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(API_ENDPOINTS.auth.login, { email, password }).pipe(
      tap({
        next: (res) => {
          this.setSession(res);
          this.toast.success(`Welcome back, ${res.user.name}!`);
        },
        error: (err) => {
          const msg = err.error?.message || 'Invalid email or password';
          this.toast.error(msg);
        }
      })
    );
  }

  register(name: string, email: string, password: string): Observable<any> {
    return this.http.post(API_ENDPOINTS.auth.register, { name, email, password }).pipe(
      tap({
        next: () => {
          this.toast.success('Registration successful! Logging in...');
        },
        error: (err) => {
          const msg = err.error?.message || 'Registration failed. Email might already exist.';
          this.toast.error(msg);
        }
      })
    );
  }

  refreshMe(): Observable<User> {
    return this.http.get<User>(API_ENDPOINTS.users.me).pipe(
      tap({
        next: (user) => {
          this.userSignal.set(user);
          localStorage.setItem('ecom_user', JSON.stringify(user));
        },
        error: (err) => {
          if (err.status === 401) {
            this.logout(false);
          }
        }
      })
    );
  }

  becomeSeller(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(API_ENDPOINTS.users.becomeSeller, {}).pipe(
      tap({
        next: (res) => {
          this.setSession(res);
          this.toast.success('You can now list products for sale on ApexMart.');
        },
        error: (err) => {
          const msg = err.error?.message || 'Could not upgrade account to Seller';
          this.toast.error(msg);
        }
      })
    );
  }

  logout(showToast = true): void {
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    localStorage.removeItem('ecom_token');
    localStorage.removeItem('ecom_user');
    if (showToast) {
      this.toast.info('You have logged out successfully.');
    }
  }
}
