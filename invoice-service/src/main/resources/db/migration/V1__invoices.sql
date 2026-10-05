CREATE TABLE invoices (
 id UUID PRIMARY KEY, invoice_number VARCHAR(50) NOT NULL UNIQUE, order_id UUID NOT NULL UNIQUE,
 request_hash VARCHAR(64) NOT NULL, customer_name VARCHAR(100) NOT NULL, customer_email VARCHAR(254) NOT NULL,
 address VARCHAR(1000) NOT NULL, currency VARCHAR(3) NOT NULL, total DECIMAL(24,2) NOT NULL CHECK(total>0), issued_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE invoice_items (
 invoice_id UUID NOT NULL REFERENCES invoices(id), line_no INT4 NOT NULL,
 product_id UUID NOT NULL, seller_id UUID NOT NULL, name VARCHAR(200) NOT NULL,
 quantity INT4 NOT NULL CHECK(quantity>0), unit_price DECIMAL(19,2) NOT NULL CHECK(unit_price>0), PRIMARY KEY(invoice_id,line_no)
);
