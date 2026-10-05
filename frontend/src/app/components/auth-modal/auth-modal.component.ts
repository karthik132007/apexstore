import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-auth-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auth-modal.component.html',
  styleUrls: ['./auth-modal.component.css']
})
export class AuthModalComponent {
  private readonly auth = inject(AuthService);

  @Output() close = new EventEmitter<void>();

  isLoginMode = signal(true);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  // Form inputs
  name = '';
  email = '';
  password = '';

  setLoginMode(login: boolean): void {
    this.isLoginMode.set(login);
    this.errorMessage.set(null);
  }

  onSubmit(): void {
    this.errorMessage.set(null);

    if (this.isLoginMode()) {
      if (!this.email || !this.password) {
        this.errorMessage.set('Please provide both email and password.');
        return;
      }

      this.isLoading.set(true);
      this.auth.login(this.email, this.password).subscribe({
        next: () => {
          this.isLoading.set(false);
          this.close.emit();
        },
        error: (err) => {
          this.isLoading.set(false);
          this.errorMessage.set(err.error?.message || 'Login failed. Check your credentials.');
        }
      });
    } else {
      if (!this.name || !this.email || !this.password) {
        this.errorMessage.set('Please fill out all fields.');
        return;
      }

      this.isLoading.set(true);
      this.auth.register(this.name, this.email, this.password).subscribe({
        next: () => {
          // Immediately log in with new credentials
          this.auth.login(this.email, this.password).subscribe({
            next: () => {
              this.isLoading.set(false);
              this.close.emit();
            },
            error: () => {
              this.isLoading.set(false);
              this.isLoginMode.set(true);
            }
          });
        },
        error: (err) => {
          this.isLoading.set(false);
          this.errorMessage.set(err.error?.message || 'Registration failed. Email might already exist.');
        }
      });
    }
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close.emit();
    }
  }
}
