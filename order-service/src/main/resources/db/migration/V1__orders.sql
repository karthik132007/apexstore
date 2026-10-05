CREATE TABLE orders (
 id UUID PRIMARY KEY, buyer_id UUID NOT NULL, status VARCHAR(20) NOT NULL,
 idempotency_key VARCHAR(100) NOT NULL, request_hash VARCHAR(64) NOT NULL,
 customer_name VARCHAR(100) NOT NULL, customer_email VARCHAR(254) NOT NULL, address VARCHAR(1000) NOT NULL,
 currency VARCHAR(3) NOT NULL, total DECIMAL(24,2) NOT NULL CHECK(total>=0),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL, invoice_requested BOOLEAN NOT NULL, invoice_xml TEXT,
 version BIGINT NOT NULL, UNIQUE(buyer_id,idempotency_key),
 CHECK(status IN ('PENDING','CONFIRMED','FAILED','CANCEL_PENDING','CANCELLED'))
);
CREATE INDEX orders_buyer ON orders(buyer_id,created_at);
CREATE TABLE order_items (
 order_id UUID NOT NULL REFERENCES orders(id), line_no INT4 NOT NULL, product_id UUID NOT NULL,
 seller_id UUID, name VARCHAR(200), unit_price DECIMAL(19,2), quantity INT4 NOT NULL CHECK(quantity>0),
 PRIMARY KEY(order_id,line_no), UNIQUE(order_id,product_id)
);
CREATE TABLE order_workflows (
 order_id UUID PRIMARY KEY REFERENCES orders(id), step VARCHAR(30) NOT NULL, retry_count INT4 NOT NULL,
 next_attempt TIMESTAMP WITH TIME ZONE NOT NULL, lease_until TIMESTAMP WITH TIME ZONE,
 lease_token UUID, last_error VARCHAR(100)
);
CREATE INDEX workflows_due ON order_workflows(step,next_attempt);
