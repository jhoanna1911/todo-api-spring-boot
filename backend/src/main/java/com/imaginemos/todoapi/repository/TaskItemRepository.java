package com.imaginemos.todoapi.repository;

import com.imaginemos.todoapi.entity.TaskItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {
}
