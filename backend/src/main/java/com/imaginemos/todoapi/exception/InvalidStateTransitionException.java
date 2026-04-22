package com.imaginemos.todoapi.exception;

import com.imaginemos.todoapi.entity.TaskStatus;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(TaskStatus from, TaskStatus to) {
        super("Invalid state transition: %s -> %s".formatted(from, to));
    }
}
