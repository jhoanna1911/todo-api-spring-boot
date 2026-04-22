package com.imaginemos.todoapi.dto.response;

public record TaskItemResponse(
        Long id,
        String description,
        boolean completed
) {
}
