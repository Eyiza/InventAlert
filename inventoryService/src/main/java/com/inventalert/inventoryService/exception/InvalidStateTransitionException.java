package com.inventalert.inventoryService.exception;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String entity, String currentState, String action) {
        super("Unable to " + action.toLowerCase() + " — this " + entity.toLowerCase()
                + " is currently " + currentState.toLowerCase().replace("_", " ") + ".");
    }
}
