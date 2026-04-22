package com.imaginemos.todoapi.service;

import com.imaginemos.todoapi.dto.request.CreateTaskRequest;
import com.imaginemos.todoapi.dto.request.UpdateTaskRequest;
import com.imaginemos.todoapi.dto.response.TaskResponse;
import com.imaginemos.todoapi.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskService {

    TaskResponse createTask(CreateTaskRequest request);

    TaskResponse updateTask(Long id, UpdateTaskRequest request);

    void deleteTask(Long id);

    TaskResponse getTaskById(Long id);

    Page<TaskResponse> getAllTasks(Pageable pageable);

    Page<TaskResponse> searchTasks(String query, Pageable pageable);

    TaskResponse updateTaskStatus(Long id, TaskStatus newStatus);

    List<TaskResponse> getPendingTasks();

    List<TaskResponse> getOverdueTasks();

    TaskResponse updateTaskItem(Long taskId, Long itemId, boolean completed);
}
