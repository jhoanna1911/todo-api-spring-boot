package com.imaginemos.todoapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.imaginemos.todoapi.dto.request.CreateTaskRequest;
import com.imaginemos.todoapi.dto.request.UpdateTaskRequest;
import com.imaginemos.todoapi.dto.request.UpdateTaskStatusRequest;
import com.imaginemos.todoapi.entity.Task;
import com.imaginemos.todoapi.entity.TaskItem;
import com.imaginemos.todoapi.entity.TaskStatus;
import com.imaginemos.todoapi.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "server.servlet.context-path=/api"
})
class TaskControllerIntegrationTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private TaskRepository taskRepository;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        taskRepository.deleteAll();
    }

    private Task persistTask(TaskStatus status, LocalDateTime executionDate) {
        Task t = new Task();
        t.setTitle("Sample");
        t.setDescription("d");
        t.setStatus(status);
        t.setExecutionDate(executionDate);
        return taskRepository.saveAndFlush(t);
    }

    @Test
    void createTask_returns201WithLocation() throws Exception {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .title("Write tests")
                .description("Cover service and controller")
                .itemDescriptions(List.of("a", "b"))
                .build();

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/tasks/")))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.itemsTotalCount").value(2));
    }

    @Test
    void createTask_returns400WhenTitleMissing() throws Exception {
        CreateTaskRequest req = CreateTaskRequest.builder().description("x").build();

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void getById_returns200() throws Exception {
        Task t = persistTask(TaskStatus.SCHEDULED, null);

        mockMvc.perform(get("/api/v1/tasks/" + t.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(t.getId()));
    }

    @Test
    void getById_returns404WhenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateTask_returns200() throws Exception {
        Task t = persistTask(TaskStatus.SCHEDULED, null);
        UpdateTaskRequest req = UpdateTaskRequest.builder().title("Updated").build();

        mockMvc.perform(put("/api/v1/tasks/" + t.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void deleteTask_returns204() throws Exception {
        Task t = persistTask(TaskStatus.SCHEDULED, null);

        mockMvc.perform(delete("/api/v1/tasks/" + t.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void listTasks_paginates() throws Exception {
        persistTask(TaskStatus.SCHEDULED, null);
        persistTask(TaskStatus.IN_PROGRESS, null);

        mockMvc.perform(get("/api/v1/tasks").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void searchTasks_filtersByTitle() throws Exception {
        Task t = persistTask(TaskStatus.SCHEDULED, null);
        t.setTitle("Unique-Search-Token");
        taskRepository.saveAndFlush(t);
        persistTask(TaskStatus.SCHEDULED, null);

        mockMvc.perform(get("/api/v1/tasks/search").param("query", "Unique-Search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateStatus_validTransition() throws Exception {
        Task t = persistTask(TaskStatus.SCHEDULED, null);
        UpdateTaskStatusRequest req = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

        mockMvc.perform(patch("/api/v1/tasks/" + t.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void updateStatus_invalidTransitionReturns400() throws Exception {
        Task t = persistTask(TaskStatus.COMPLETED, null);
        UpdateTaskStatusRequest req = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

        mockMvc.perform(patch("/api/v1/tasks/" + t.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid state transition")));
    }

    @Test
    void pending_returnsScheduledAndInProgress() throws Exception {
        persistTask(TaskStatus.SCHEDULED, null);
        persistTask(TaskStatus.IN_PROGRESS, null);
        persistTask(TaskStatus.COMPLETED, null);

        mockMvc.perform(get("/api/v1/tasks/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void overdue_returnsOnlyOverdueScheduled() throws Exception {
        persistTask(TaskStatus.SCHEDULED, LocalDateTime.now().minusDays(1));
        persistTask(TaskStatus.SCHEDULED, LocalDateTime.now().plusDays(1));

        mockMvc.perform(get("/api/v1/tasks/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void updateItem_togglesCompleted() throws Exception {
        Task t = persistTask(TaskStatus.IN_PROGRESS, null);
        TaskItem item = new TaskItem();
        item.setDescription("x");
        item.setCompleted(false);
        t.addItem(item);
        taskRepository.saveAndFlush(t);
        Long itemId = t.getItems().get(0).getId();

        mockMvc.perform(patch("/api/v1/tasks/" + t.getId() + "/items/" + itemId)
                        .param("completed", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemsCompletedCount").value(1));
    }
}
