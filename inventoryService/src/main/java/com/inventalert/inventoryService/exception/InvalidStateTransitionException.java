package com.inventalert.inventoryService.exception;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String entity, String currentState, String action) {
        super("Cannot " + action + " a " + entity + " in state: " + currentState);
    }
}
