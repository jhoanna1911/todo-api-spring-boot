package com.imaginemos.todoapi.repository;

import com.imaginemos.todoapi.entity.Task;
import com.imaginemos.todoapi.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    List<Task> findByStatusIn(List<TaskStatus> statuses);

    @Query("""
           SELECT t FROM Task t
           WHERE t.status = :status
             AND t.executionDate IS NOT NULL
             AND t.executionDate < :now
           """)
    List<Task> findOverdue(@Param("status") TaskStatus status,
                           @Param("now") LocalDateTime now);

    @Query("""
           SELECT t FROM Task t
           WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :query, '%'))
           """)
    Page<Task> search(@Param("query") String query, Pageable pageable);
}
