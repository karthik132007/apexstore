# Verification record

Verified on 4 October 2026 using the local Java applications and the connected CockroachDB 26.2 cluster.

## Automated integration tests

16 tests passed with zero failures or errors:

| Service | Tests | Main scenarios |
|---|---:|---|
| User | 2 | Password hashing, registration cannot grant admin, login, seller role, unauthorized profile |
| Product | 5 | Idempotent reservations, atomic rollback, concurrent purchases, ownership/admin restrictions, admin creation |
| Invoice | 3 | Real HTTP SOAP request, stable invoice identity, WSDL, internal authentication, SOAP validation faults |
| Order | 6 | Checkout idempotency, interrupted confirmation recovery, cancellation, buyer ownership, cached invoice, invoice outage |

These tests use isolated H2 databases with the versioned schema migrations. Order tests simulate remote failures using mocked clients; they are not a claim of live process-crash testing.

## Live CockroachDB verification

All five applications reported UP through their health endpoints. `scripts/Smoke-Test.ps1` passed 16 live assertions through the API Gateway:

- Seller registration and seller enablement.
- Seller creates product and sets inventory.
- Seller cannot use admin product creation.
- Customer cannot modify a seller listing.
- Public pagination and sorting.
- Admin creates a product and edits a seller's product.
- Checkout confirms and calculates prices on the server.
- Repeated checkout returns the same order.
- Stock is deducted once.
- Another user cannot read the buyer's order.
- REST invoice request calls the live SOAP service and repeated generation returns one invoice.
- Invoice generation protects against cancellation.
- Cancellation restores stock once.
- Insufficient stock rejects the order.
- Invalid page size is rejected.

The actual IDs, order data, invoice data and timestamp are in `verification.json`. Demo users, products, confirmed/cancelled/failed orders and the invoice were retained in the application databases. No payment provider was used.

## Compatibility fixes verified

- Java JDBC uses its trusted CA store with `sslmode=verify-full`; encryption and hostname verification remain enabled.
- Flyway 13.9.0's dedicated CockroachDB adapter replaces the older adapter that queried restricted internal tables. No `allow_unsafe_internals` setting was enabled. Flyway reports that CockroachDB 26.2 is newer than its latest verified version 26.1; the actual migrations and application operations passed on this cluster.
- Integer schema columns use INT4 to match Java integer fields on CockroachDB. Money totals use DECIMAL(24,2) to accommodate multi-item orders without floating-point arithmetic.
- The Windows launcher uses the actual JDK executable and records process launch times for safe shutdown.

## Remaining submission work

Local-page screenshot capture was blocked by the app browser. See `screenshots/README.md` for the specific Postman/SoapUI captures needed for the academic report. The frontend remains a separate task.
