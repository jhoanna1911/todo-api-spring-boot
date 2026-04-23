package com.imaginemos.todoapi.controller;

import com.imaginemos.todoapi.dto.request.CreateTaskItemRequest;
import com.imaginemos.todoapi.dto.request.CreateTaskRequest;
import com.imaginemos.todoapi.dto.request.UpdateTaskItemRequest;
import com.imaginemos.todoapi.dto.request.UpdateTaskRequest;
import com.imaginemos.todoapi.dto.request.UpdateTaskStatusRequest;
import com.imaginemos.todoapi.dto.response.PagedResponse;
import com.imaginemos.todoapi.dto.response.TaskResponse;
import com.imaginemos.todoapi.entity.TaskStatus;
import com.imaginemos.todoapi.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Manage tasks and their items")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create task", description = "Creates a new task with initial status SCHEDULED")
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody CreateTaskRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        TaskResponse created = taskService.createTask(request);
        URI location = uriBuilder.path("/v1/tasks/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public TaskResponse getById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task", description = "Updates only non-null fields from the request")
    public TaskResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete task")
    public void delete(@PathVariable Long id) {
        taskService.deleteTask(id);
    }

    @GetMapping
    @Operation(summary = "List tasks (paged)",
               description = "Params: page, size, sort (e.g. sort=executionDate,desc). " +
                             "Optional status filter: ?status=SCHEDULED | IN_PROGRESS | COMPLETED | CANCELLED")
    public PagedResponse<TaskResponse> list(
            @RequestParam(name = "status", required = false) TaskStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Page<TaskResponse> page = (status == null)
                ? taskService.getAllTasks(pageable)
                : taskService.getTasksByStatus(status, pageable);
        return PagedResponse.from(page, t -> t);
    }

    @GetMapping("/search")
    @Operation(summary = "Search tasks",
               description = "Case-insensitive match on title, task description or item descriptions. " +
                             "Optional status filter (SCHEDULED | IN_PROGRESS | COMPLETED | CANCELLED). " +
                             "Paginated: page, size, sort.")
    public PagedResponse<TaskResponse> search(
            @RequestParam(name = "query", required = false, defaultValue = "") String query,
            @RequestParam(name = "status", required = false) TaskStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return PagedResponse.from(taskService.searchTasks(query, status, pageable), t -> t);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change status",
               description = "Valid transitions: SCHEDULED→IN_PROGRESS, IN_PROGRESS→COMPLETED, IN_PROGRESS→CANCELLED")
    public TaskResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return taskService.updateTaskStatus(id, request.getNewStatus());
    }

    @GetMapping("/pending")
    @Operation(summary = "Pending tasks (paged)",
               description = "Tasks in SCHEDULED or IN_PROGRESS status. Params: page, size, sort.")
    public PagedResponse<TaskResponse> pending(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return PagedResponse.from(taskService.getPendingTasks(pageable), t -> t);
    }

    @GetMapping("/overdue")
    @Operation(summary = "Overdue tasks (paged)",
               description = "SCHEDULED tasks whose executionDate is in the past. Params: page, size, sort.")
    public PagedResponse<TaskResponse> overdue(
            @PageableDefault(size = 20, sort = "executionDate") Pageable pageable
    ) {
        return PagedResponse.from(taskService.getOverdueTasks(pageable), t -> t);
    }

    @PatchMapping("/{taskId}/items/{itemId}")
    @Operation(summary = "Mark item as completed/pending")
    public TaskResponse updateItem(
            @PathVariable Long taskId,
            @PathVariable Long itemId,
            @RequestParam boolean completed
    ) {
        return taskService.updateTaskItem(taskId, itemId, completed);
    }

    @PostMapping("/{taskId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add item to a task")
    public TaskResponse addItem(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateTaskItemRequest request
    ) {
        return taskService.addTaskItem(taskId, request);
    }

    @PutMapping("/{taskId}/items/{itemId}")
    @Operation(summary = "Edit item (description and/or completed)")
    public TaskResponse editItem(
            @PathVariable Long taskId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateTaskItemRequest request
    ) {
        return taskService.editTaskItem(taskId, itemId, request);
    }

    @DeleteMapping("/{taskId}/items/{itemId}")
    @Operation(summary = "Remove item from a task")
    public TaskResponse deleteItem(
            @PathVariable Long taskId,
            @PathVariable Long itemId
    ) {
        return taskService.deleteTaskItem(taskId, itemId);
    }
}
