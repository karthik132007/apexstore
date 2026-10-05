CREATE TABLE products (
 id UUID PRIMARY KEY, seller_id UUID NOT NULL, sku VARCHAR(64) NOT NULL UNIQUE,
 name VARCHAR(200) NOT NULL, description VARCHAR(4000) NOT NULL, category VARCHAR(100) NOT NULL,
 price DECIMAL(19,2) NOT NULL CHECK(price>0), currency VARCHAR(3) NOT NULL,
 image_url VARCHAR(2048), active BOOLEAN NOT NULL, stock_on_hand INT4 NOT NULL CHECK(stock_on_hand>=0),
 reserved INT4 NOT NULL CHECK(reserved>=0 AND reserved<=stock_on_hand), version BIGINT NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX products_seller ON products(seller_id,created_at);
CREATE INDEX products_catalogue ON products(active,category,price);
CREATE TABLE stock_reservations (
 order_id UUID PRIMARY KEY, status VARCHAR(20) NOT NULL, request_hash VARCHAR(64) NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CHECK(status IN ('RESERVED','CONFIRMED','RELEASED','CANCELLED'))
);
CREATE TABLE stock_reservation_items (
 order_id UUID NOT NULL REFERENCES stock_reservations(order_id), line_no INT4 NOT NULL,
 product_id UUID NOT NULL REFERENCES products(id), seller_id UUID NOT NULL,
 name VARCHAR(200) NOT NULL, unit_price DECIMAL(19,2) NOT NULL, quantity INT4 NOT NULL CHECK(quantity>0),
 PRIMARY KEY(order_id,line_no), UNIQUE(order_id,product_id)
);
