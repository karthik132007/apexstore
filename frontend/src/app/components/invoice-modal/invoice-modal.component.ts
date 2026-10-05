import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DialogDirective } from '../../directives/dialog.directive';
import { CommonModule } from '@angular/common';
import { Invoice } from '../../models/ecom.models';

@Component({
  selector: 'app-invoice-modal',
  standalone: true,
  imports: [DialogDirective, CommonModule],
  templateUrl: './invoice-modal.component.html',
  styleUrls: ['./invoice-modal.component.css']
})
export class InvoiceModalComponent {
  @Input({ required: true }) invoice!: Invoice;
  @Output() close = new EventEmitter<void>();

  printInvoice(): void {
    window.print();
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close.emit();
    }
  }
}
