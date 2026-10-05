import { AfterViewInit, Directive, ElementRef, OnDestroy, inject } from '@angular/core';

@Directive({ selector: '[appReveal]', standalone: true })
export class RevealDirective implements AfterViewInit, OnDestroy {
  private readonly element: HTMLElement = inject(ElementRef).nativeElement;
  private observer?: IntersectionObserver;

  ngAfterViewInit(): void {
    if (!('IntersectionObserver' in window) || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    this.element.classList.add('reveal-pending');
    this.observer = new IntersectionObserver(entries => {
      if (entries.some(entry => entry.isIntersecting)) {
        this.element.classList.remove('reveal-pending');
        this.observer?.disconnect();
      }
    }, { threshold: 0.08 });
    this.observer.observe(this.element);
  }

  ngOnDestroy(): void { this.observer?.disconnect(); }
}
