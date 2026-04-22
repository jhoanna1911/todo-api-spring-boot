package com.imaginemos.todoapi.mapper;

import com.imaginemos.todoapi.dto.request.CreateTaskRequest;
import com.imaginemos.todoapi.dto.request.UpdateTaskRequest;
import com.imaginemos.todoapi.dto.response.TaskItemResponse;
import com.imaginemos.todoapi.dto.response.TaskResponse;
import com.imaginemos.todoapi.entity.Task;
import com.imaginemos.todoapi.entity.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final TaskItemMapper taskItemMapper;

    public Task toEntity(CreateTaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setExecutionDate(request.getExecutionDate());
        task.setStatus(TaskStatus.SCHEDULED);

        Optional.ofNullable(request.getItemDescriptions())
                .orElseGet(List::of)
                .stream()
                .map(taskItemMapper::fromDescription)
                .forEach(task::addItem);

        return task;
    }

    public void updateEntity(UpdateTaskRequest request, Task task) {
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getExecutionDate() != null) {
            task.setExecutionDate(request.getExecutionDate());
        }
    }

    public TaskResponse toResponse(Task task) {
        List<TaskItemResponse> itemResponses = task.getItems().stream()
                .map(taskItemMapper::toResponse)
                .toList();

        int total = itemResponses.size();
        int completed = (int) itemResponses.stream().filter(TaskItemResponse::completed).count();

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getExecutionDate(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                itemResponses,
                isOverdue(task),
                completed,
                total
        );
    }

    private boolean isOverdue(Task task) {
        return task.getStatus() == TaskStatus.SCHEDULED
                && task.getExecutionDate() != null
                && task.getExecutionDate().isBefore(LocalDateTime.now());
    }
}
