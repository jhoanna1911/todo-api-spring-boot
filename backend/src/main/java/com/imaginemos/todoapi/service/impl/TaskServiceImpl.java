package com.imaginemos.todoapi.service.impl;

import com.imaginemos.todoapi.dto.request.CreateTaskItemRequest;
import com.imaginemos.todoapi.dto.request.CreateTaskRequest;
import com.imaginemos.todoapi.dto.request.UpdateTaskItemRequest;
import com.imaginemos.todoapi.dto.request.UpdateTaskRequest;
import com.imaginemos.todoapi.dto.response.TaskResponse;
import com.imaginemos.todoapi.entity.Task;
import com.imaginemos.todoapi.entity.TaskItem;
import com.imaginemos.todoapi.entity.TaskStatus;
import com.imaginemos.todoapi.exception.InvalidStateTransitionException;
import com.imaginemos.todoapi.exception.ResourceNotFoundException;
import com.imaginemos.todoapi.mapper.TaskMapper;
import com.imaginemos.todoapi.repository.TaskRepository;
import com.imaginemos.todoapi.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskServiceImpl implements TaskService {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            TaskStatus.SCHEDULED, Set.of(TaskStatus.IN_PROGRESS),
            TaskStatus.IN_PROGRESS, Set.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED),
            TaskStatus.COMPLETED, Set.of(),
            TaskStatus.CANCELLED, Set.of()
    );

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Override
    public TaskResponse createTask(CreateTaskRequest request) {
        Task task = taskMapper.toEntity(request);
        Task saved = taskRepository.save(task);
        log.info("Created task id={}", saved.getId());
        return taskMapper.toResponse(saved);
    }

    @Override
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        Task task = findTaskOrThrow(id);
        taskMapper.updateEntity(request, task);
        return taskMapper.toResponse(task);
    }

    @Override
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Task", id);
        }
        taskRepository.deleteById(id);
        log.info("Deleted task id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        return taskMapper.toResponse(findTaskOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(Pageable pageable) {
        return taskRepository.findAll(pageable).map(taskMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByStatus(TaskStatus status, Pageable pageable) {
        return taskRepository.findByStatus(status, pageable).map(taskMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> searchTasks(String query, TaskStatus status, Pageable pageable) {
        String q = query == null ? "" : query.trim();
        return taskRepository.search(q, status, pageable).map(taskMapper::toResponse);
    }

    @Override
    public TaskResponse updateTaskStatus(Long id, TaskStatus newStatus) {
        Task task = findTaskOrThrow(id);
        TaskStatus current = task.getStatus();

        if (current == newStatus) {
            return taskMapper.toResponse(task);
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(newStatus)) {
            throw new InvalidStateTransitionException(current, newStatus);
        }

        task.setStatus(newStatus);
        log.info("Task id={} status: {} -> {}", id, current, newStatus);
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getPendingTasks(Pageable pageable) {
        return taskRepository
                .findByStatusIn(List.of(TaskStatus.SCHEDULED, TaskStatus.IN_PROGRESS), pageable)
                .map(taskMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getOverdueTasks(Pageable pageable) {
        return taskRepository
                .findOverdue(TaskStatus.SCHEDULED, LocalDateTime.now(), pageable)
                .map(taskMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countOverdue() {
        return taskRepository.countOverdue(TaskStatus.SCHEDULED, LocalDateTime.now());
    }

    @Override
    public TaskResponse updateTaskItem(Long taskId, Long itemId, boolean completed) {
        Task task = findTaskOrThrow(taskId);
        TaskItem item = findItemOrThrow(task, itemId);
        item.setCompleted(completed);
        return taskMapper.toResponse(task);
    }

    @Override
    public TaskResponse addTaskItem(Long taskId, CreateTaskItemRequest request) {
        Task task = findTaskOrThrow(taskId);

        TaskItem item = new TaskItem();
        item.setDescription(request.getDescription());
        item.setCompleted(false);
        task.addItem(item);

        log.info("Added item to task id={}", taskId);
        return taskMapper.toResponse(task);
    }

    @Override
    public TaskResponse editTaskItem(Long taskId, Long itemId, UpdateTaskItemRequest request) {
        Task task = findTaskOrThrow(taskId);
        TaskItem item = findItemOrThrow(task, itemId);

        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getCompleted() != null) {
            item.setCompleted(request.getCompleted());
        }
        return taskMapper.toResponse(task);
    }

    @Override
    public TaskResponse deleteTaskItem(Long taskId, Long itemId) {
        Task task = findTaskOrThrow(taskId);
        TaskItem item = findItemOrThrow(task, itemId);
        task.removeItem(item);
        log.info("Removed item id={} from task id={}", itemId, taskId);
        return taskMapper.toResponse(task);
    }

    private Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Task", id));
    }

    private TaskItem findItemOrThrow(Task task, Long itemId) {
        return task.getItems().stream()
                .filter(i -> itemId.equals(i.getId()))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("TaskItem", itemId));
    }
}
