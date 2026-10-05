import { AfterViewInit, Directive, ElementRef, EventEmitter, HostListener, OnDestroy, Output, inject } from '@angular/core';

@Directive({ selector: '[appDialog]', standalone: true })
export class DialogDirective implements AfterViewInit, OnDestroy {
  private readonly element: HTMLElement = inject(ElementRef).nativeElement;
  private previousFocus = document.activeElement as HTMLElement | null;
  private previousOverflow = document.body.style.overflow;
  @Output() appDialogEscape = new EventEmitter<void>();

  ngAfterViewInit(): void {
    document.body.style.overflow = 'hidden';
    this.element.focus();
  }

  @HostListener('keydown', ['$event']) onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') { event.stopPropagation(); this.appDialogEscape.emit(); }
    if (event.key !== 'Tab') return;
    const items = Array.from(this.element.querySelectorAll<HTMLElement>('a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex="0"]')).filter(item => item.getClientRects().length > 0);
    const first = items[0], last = items[items.length - 1];
    if (!first) { event.preventDefault(); return; }
    if (event.shiftKey && (document.activeElement === first || document.activeElement === this.element)) { event.preventDefault(); last.focus(); }
    else if (!event.shiftKey && (document.activeElement === last || document.activeElement === this.element)) { event.preventDefault(); first.focus(); }
  }

  ngOnDestroy(): void {
    document.body.style.overflow = this.previousOverflow;
    this.previousFocus?.focus();
  }
}
