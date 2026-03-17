package com.flipkart.minutes.exception;

/**
 * Root application exception for Flipkart Minutes.
 *
 * All business-rule violations (e.g. item out of stock, invalid state
 * transitions, entity not found) are thrown as this exception so the
 * DemoRunner and callers can catch it in a single place.
 */
public class FlipkartMinutesException extends RuntimeException {

    public FlipkartMinutesException(String message) {
        super(message);
    }

    public FlipkartMinutesException(String message, Throwable cause) {
        super(message, cause);
    }
}
