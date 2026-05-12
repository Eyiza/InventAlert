CREATE DATABASE IF NOT EXISTS inventalert_analytics;

CREATE TABLE IF NOT EXISTS inventalert_analytics.company_events (
    eventId      String,
    companyId    String,
    companyName  String,
    adminEmail   String,
    eventType    String,
    eventTime    DateTime64(3, 'UTC'),
    ingestedAt   DateTime64(3, 'UTC') DEFAULT now64()
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(eventTime)
ORDER BY (companyId, eventTime)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS inventalert_analytics.stock_movement_events (
    eventId      String,
    companyId    String,
    movementId   String,
    productId    String,
    warehouseId  String,
    movementType String,
    quantity     Int32,
    eventTime    DateTime64(3, 'UTC'),
    ingestedAt   DateTime64(3, 'UTC') DEFAULT now64()
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(eventTime)
ORDER BY (companyId, warehouseId, productId, eventTime)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS inventalert_analytics.alert_events (
    eventId      String,
    companyId    String,
    alertId      String,
    productId    String,
    warehouseId  String,
    stockAtAlert Int32,
    threshold    Int32,
    eventTime    DateTime64(3, 'UTC'),
    ingestedAt   DateTime64(3, 'UTC') DEFAULT now64()
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(eventTime)
ORDER BY (companyId, warehouseId, eventTime)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS inventalert_analytics.transfer_events (
    eventId         String,
    companyId       String,
    suggestionId    String,
    productId       String,
    fromWarehouseId String,
    toWarehouseId   String,
    quantity        Int32,
    distanceKm      Nullable(Float64),
    status          String,
    eventTime       DateTime64(3, 'UTC'),
    ingestedAt      DateTime64(3, 'UTC') DEFAULT now64()
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(eventTime)
ORDER BY (companyId, suggestionId, eventTime)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS inventalert_analytics.reconciliation_events (
    eventId          String,
    companyId        String,
    reconciliationId String,
    warehouseId      String,
    eventTime        DateTime64(3, 'UTC'),
    ingestedAt       DateTime64(3, 'UTC') DEFAULT now64()
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(eventTime)
ORDER BY (companyId, warehouseId, eventTime)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS inventalert_analytics.notification_events (
    eventId      String,
    companyId    String,
    userId       String,
    notifType    String,
    referenceId  String,
    eventTime    DateTime64(3, 'UTC'),
    ingestedAt   DateTime64(3, 'UTC') DEFAULT now64()
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(eventTime)
ORDER BY (companyId, notifType, eventTime)
SETTINGS index_granularity = 8192;
