-- =====================================================================
-- V1: Core NearKirana tables
--
-- Flyway runs this file ONCE, then records it in "flyway_schema_history".
-- NEVER edit this file after it has run. To change the schema, add a new
-- file such as V3__add_something.sql.
--
-- Requires the PostGIS extension to already be enabled in this database
-- (an admin runs: CREATE EXTENSION postgis;). The app user cannot do it.
-- =====================================================================


-- ---------------------------------------------------------------------
-- users: every person who logs in (customer or shopkeeper)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mobile_number       VARCHAR(15)  NOT NULL,           -- stored as +91XXXXXXXXXX
    role                VARCHAR(20)  NOT NULL,
    full_name           VARCHAR(100),                    -- optional: filled after first login
    preferred_language  VARCHAR(10)  NOT NULL DEFAULT 'en',  -- en, hi, te, ta, kn, mr ...
    is_verified         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_mobile_number UNIQUE (mobile_number),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'SHOPKEEPER'))
);


-- ---------------------------------------------------------------------
-- shops: a Kirana shop, owned by one SHOPKEEPER user
-- ---------------------------------------------------------------------
CREATE TABLE shops (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id            BIGINT        NOT NULL,
    name                VARCHAR(150)  NOT NULL,
    description         VARCHAR(500),
    contact_number      VARCHAR(15)   NOT NULL,
    address_line        VARCHAR(255)  NOT NULL,
    village_or_city     VARCHAR(100)  NOT NULL,
    district            VARCHAR(100),
    state               VARCHAR(100)  NOT NULL,
    pincode             VARCHAR(6)    NOT NULL,
    location            GEOGRAPHY(POINT, 4326) NOT NULL,  -- longitude/latitude (PostGIS)
    is_open             BOOLEAN       NOT NULL DEFAULT TRUE,
    delivery_available  BOOLEAN       NOT NULL DEFAULT FALSE,
    delivery_radius_km  NUMERIC(5,2),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_shops_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_shops_pincode CHECK (pincode ~ '^[1-9][0-9]{5}$'),
    CONSTRAINT ck_shops_delivery_radius CHECK (delivery_radius_km IS NULL OR delivery_radius_km > 0)
);

-- Fast lookup: "all shops of this shopkeeper"
CREATE INDEX idx_shops_owner_id ON shops (owner_id);
-- Fast lookup: "shops within 5 km" (GiST = spatial index)
CREATE INDEX idx_shops_location ON shops USING GIST (location);


-- ---------------------------------------------------------------------
-- categories: Rice & Grains, Pulses, Oil & Ghee ...
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL,      -- URL/code friendly: rice-grains
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_categories_slug UNIQUE (slug)
);


-- ---------------------------------------------------------------------
-- products: the common product catalog (same "Tata Salt 1 kg" for all shops)
-- ---------------------------------------------------------------------
CREATE TABLE products (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id  BIGINT        NOT NULL,
    name         VARCHAR(150)  NOT NULL,
    brand        VARCHAR(100),
    unit         VARCHAR(30)   NOT NULL,     -- "1 kg", "500 g", "1 L", "1 piece"
    image_url    VARCHAR(500),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_products_category_id ON products (category_id);


-- ---------------------------------------------------------------------
-- shop_products: what a specific shop sells, at ITS OWN price and stock
-- (links shops <-> products: a "many-to-many" relationship)
-- ---------------------------------------------------------------------
CREATE TABLE shop_products (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    shop_id         BIGINT         NOT NULL,
    product_id      BIGINT         NOT NULL,
    price           NUMERIC(10,2)  NOT NULL,   -- selling price in rupees
    mrp             NUMERIC(10,2),             -- printed maximum retail price
    stock_quantity  INTEGER        NOT NULL DEFAULT 0,
    is_available    BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),

    -- Deleting a shop removes its product listings too
    CONSTRAINT fk_shop_products_shop    FOREIGN KEY (shop_id)    REFERENCES shops (id) ON DELETE CASCADE,
    CONSTRAINT fk_shop_products_product FOREIGN KEY (product_id) REFERENCES products (id),
    -- A shop lists each product only once
    CONSTRAINT uq_shop_products_shop_product UNIQUE (shop_id, product_id),
    CONSTRAINT ck_shop_products_price CHECK (price >= 0),
    CONSTRAINT ck_shop_products_mrp   CHECK (mrp IS NULL OR mrp >= price),
    CONSTRAINT ck_shop_products_stock CHECK (stock_quantity >= 0)
);

-- Fast lookup: "which shops sell this product?" (price comparison)
CREATE INDEX idx_shop_products_product_id ON shop_products (product_id);
