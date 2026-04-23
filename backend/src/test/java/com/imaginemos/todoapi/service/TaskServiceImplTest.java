package com.imaginemos.todoapi.service;

import com.imaginemos.todoapi.dto.request.CreateTaskRequest;
import com.imaginemos.todoapi.dto.request.UpdateTaskRequest;
import com.imaginemos.todoapi.dto.response.TaskResponse;
import com.imaginemos.todoapi.entity.Task;
import com.imaginemos.todoapi.entity.TaskItem;
import com.imaginemos.todoapi.entity.TaskStatus;
import com.imaginemos.todoapi.exception.InvalidStateTransitionException;
import com.imaginemos.todoapi.exception.ResourceNotFoundException;
import com.imaginemos.todoapi.mapper.TaskItemMapper;
import com.imaginemos.todoapi.mapper.TaskMapper;
import com.imaginemos.todoapi.repository.TaskRepository;
import com.imaginemos.todoapi.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl service;

    private TaskMapper taskMapper;

    @BeforeEach
    void setUp() {
        // Mapper is a real bean; inject manually so computed fields are real.
        taskMapper = new TaskMapper(new TaskItemMapper());
        service = new TaskServiceImpl(taskRepository, taskMapper);
    }

    private Task sampleTask(Long id, TaskStatus status) {
        Task t = new Task();
        t.setId(id);
        t.setTitle("Title " + id);
        t.setDescription("Desc");
        t.setStatus(status);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    @Test
    void createTask_returnsScheduledWithItems() {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .title("New")
                .description("d")
                .itemDescriptions(List.of("a", "b"))
                .build();

        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            t.setCreatedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });

        TaskResponse res = service.createTask(req);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.status()).isEqualTo(TaskStatus.SCHEDULED);
        assertThat(res.items()).hasSize(2);
        assertThat(res.itemsTotalCount()).isEqualTo(2);
        assertThat(res.itemsCompletedCount()).isZero();
    }

    @Test
    void updateTask_mergesNonNullFields() {
        Task existing = sampleTask(1L, TaskStatus.SCHEDULED);
        existing.setTitle("old");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));

        UpdateTaskRequest req = UpdateTaskRequest.builder().title("new").build();
        TaskResponse res = service.updateTask(1L, req);

        assertThat(res.title()).isEqualTo("new");
        assertThat(res.description()).isEqualTo("Desc");
    }

    @Test
    void updateTask_throwsWhenNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateTask(99L, new UpdateTaskRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteTask_throwsWhenNotFound() {
        when(taskRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.deleteTask(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(taskRepository, never()).deleteById(any());
    }

    @Test
    void deleteTask_deletesWhenExists() {
        when(taskRepository.existsById(1L)).thenReturn(true);
        service.deleteTask(1L);
        verify(taskRepository).deleteById(1L);
    }

    @Test
    void getTaskById_returnsResponse() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask(1L, TaskStatus.SCHEDULED)));
        TaskResponse res = service.getTaskById(1L);
        assertThat(res.id()).isEqualTo(1L);
    }

    @Test
    void getAllTasks_paginates() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of(sampleTask(1L, TaskStatus.SCHEDULED)), pageable, 1);
        when(taskRepository.findAll(pageable)).thenReturn(page);

        Page<TaskResponse> result = service.getAllTasks(pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void updateTaskStatus_validTransition() {
        Task t = sampleTask(1L, TaskStatus.SCHEDULED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));

        TaskResponse res = service.updateTaskStatus(1L, TaskStatus.IN_PROGRESS);

        assertThat(res.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateTaskStatus_invalidTransitionThrows() {
        Task t = sampleTask(1L, TaskStatus.COMPLETED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.updateTaskStatus(1L, TaskStatus.IN_PROGRESS))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void updateTaskStatus_sameStatusIsNoOp() {
        Task t = sampleTask(1L, TaskStatus.SCHEDULED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));

        TaskResponse res = service.updateTaskStatus(1L, TaskStatus.SCHEDULED);
        assertThat(res.status()).isEqualTo(TaskStatus.SCHEDULED);
    }

    @Test
    void getPendingTasks_filtersScheduledAndInProgress() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> page = new PageImpl<>(
                List.of(sampleTask(1L, TaskStatus.SCHEDULED), sampleTask(2L, TaskStatus.IN_PROGRESS)),
                pageable, 2);
        when(taskRepository.findByStatusIn(anyList(), eq(pageable))).thenReturn(page);

        Page<TaskResponse> result = service.getPendingTasks(pageable);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getOverdueTasks_callsRepositoryWithScheduled() {
        Pageable pageable = PageRequest.of(0, 20);
        Task overdue = sampleTask(1L, TaskStatus.SCHEDULED);
        overdue.setExecutionDate(LocalDateTime.now().minusDays(1));
        Page<Task> page = new PageImpl<>(List.of(overdue), pageable, 1);
        when(taskRepository.findOverdue(eq(TaskStatus.SCHEDULED), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(page);

        Page<TaskResponse> result = service.getOverdueTasks(pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isOverdue()).isTrue();
    }

    @Test
    void updateTaskItem_updatesCompletedFlag() {
        Task t = sampleTask(1L, TaskStatus.IN_PROGRESS);
        TaskItem item = new TaskItem();
        item.setId(10L);
        item.setDescription("x");
        item.setCompleted(false);
        t.addItem(item);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));

        TaskResponse res = service.updateTaskItem(1L, 10L, true);

        assertThat(res.items().get(0).completed()).isTrue();
        assertThat(res.itemsCompletedCount()).isEqualTo(1);
    }

    @Test
    void updateTaskItem_throwsWhenItemNotInTask() {
        Task t = sampleTask(1L, TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.updateTaskItem(1L, 999L, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
