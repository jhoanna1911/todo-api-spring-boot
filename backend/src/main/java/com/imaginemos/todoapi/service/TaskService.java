package com.imaginemos.todoapi.service;

import com.imaginemos.todoapi.dto.request.CreateTaskItemRequest;
import com.imaginemos.todoapi.dto.request.CreateTaskRequest;
import com.imaginemos.todoapi.dto.request.UpdateTaskItemRequest;
import com.imaginemos.todoapi.dto.request.UpdateTaskRequest;
import com.imaginemos.todoapi.dto.response.TaskResponse;
import com.imaginemos.todoapi.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    TaskResponse createTask(CreateTaskRequest request);

    TaskResponse updateTask(Long id, UpdateTaskRequest request);

    void deleteTask(Long id);

    TaskResponse getTaskById(Long id);

    Page<TaskResponse> getAllTasks(Pageable pageable);

    Page<TaskResponse> getTasksByStatus(TaskStatus status, Pageable pageable);

    Page<TaskResponse> searchTasks(String query, TaskStatus status, Pageable pageable);

    TaskResponse updateTaskStatus(Long id, TaskStatus newStatus);

    Page<TaskResponse> getPendingTasks(Pageable pageable);

    Page<TaskResponse> getOverdueTasks(Pageable pageable);

    long countOverdue();

    TaskResponse updateTaskItem(Long taskId, Long itemId, boolean completed);

    TaskResponse addTaskItem(Long taskId, CreateTaskItemRequest request);

    TaskResponse editTaskItem(Long taskId, Long itemId, UpdateTaskItemRequest request);

    TaskResponse deleteTaskItem(Long taskId, Long itemId);
}
