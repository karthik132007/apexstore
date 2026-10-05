# ApexMart storefront

A light, monochrome Angular storefront with an editorial home page, visual category collections, a responsive product grid, and matching product, bag, account, checkout, and management screens.

## Run locally

```bash
cd frontend
npm ci
npm run dev
```

Open http://localhost:4200. Product, account, and order requests use the existing gateway at http://localhost:8080. Without the backend, the editorial storefront still renders and the catalog displays a retry state; it does not invent listings.

```bash
npm run build
```

The production output is `dist/frontend/browser`.

## Design and accessibility

- White surfaces, neutral grays, black type and controls; no gradients.
- Local Inter Variable font, original editorial hero, locally served collection imagery, and a matching favicon. Image sources, the generation prompt, and font license are documented in [asset notes](public/images/ASSETS.md).
- Product search, category selection, sorting, pagination, page-level price and stock filters, and quick-add use existing services. Superseded catalog requests are canceled.
- Scroll reveals use IntersectionObserver, disconnect when complete, and honor reduced-motion preferences. Hover and dialog motion also honor reduced motion.
- Visible keyboard focus, a skip link, labeled controls, responsive navigation, and dialog focus trapping, Escape dismissal, focus restoration, and scroll locking.

## Verification

The production build passes. Headless Chromium smoke checks passed for desktop imagery, search, category selection, sorting, price filtering/reset, quick-add/bag, product quantity controls, account dialog keyboard behavior, mobile navigation, scroll reveals, and service failures. Responsive widths checked: 390, 768, 1024, and 1440 pixels; no horizontal page overflow or JavaScript runtime errors were observed.

[Desktop preview](../docs/screenshots/storefront/desktop.png) · [Mobile preview](../docs/screenshots/storefront/mobile.png)

Preview screenshots and shopping UI checks use intercepted test product fixtures. These fixtures are not part of the app. Live backend authentication, checkout, invoices, and seller operations were not revalidated in this run.
