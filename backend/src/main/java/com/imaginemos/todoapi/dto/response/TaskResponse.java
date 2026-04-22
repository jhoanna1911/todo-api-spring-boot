package com.imaginemos.todoapi.dto.response;

import com.imaginemos.todoapi.entity.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDateTime executionDate,
        TaskStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TaskItemResponse> items,
        boolean isOverdue,
        int itemsCompletedCount,
        int itemsTotalCount
) {
}
