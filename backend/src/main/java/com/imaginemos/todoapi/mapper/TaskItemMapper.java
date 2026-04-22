package com.imaginemos.todoapi.mapper;

import com.imaginemos.todoapi.dto.response.TaskItemResponse;
import com.imaginemos.todoapi.entity.TaskItem;
import org.springframework.stereotype.Component;

@Component
public class TaskItemMapper {

    public TaskItemResponse toResponse(TaskItem item) {
        return new TaskItemResponse(
                item.getId(),
                item.getDescription(),
                item.isCompleted()
        );
    }

    public TaskItem fromDescription(String description) {
        TaskItem item = new TaskItem();
        item.setDescription(description);
        item.setCompleted(false);
        return item;
    }
}
