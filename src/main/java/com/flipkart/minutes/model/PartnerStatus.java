package com.flipkart.minutes.model;

/**
 * Availability status of a Delivery Partner.
 *
 * AVAILABLE → Partner is free and can accept a new order assignment.
 * BUSY      → Partner is currently handling an order (assigned / picked up).
 */
public enum PartnerStatus {
    AVAILABLE,
    BUSY
}
