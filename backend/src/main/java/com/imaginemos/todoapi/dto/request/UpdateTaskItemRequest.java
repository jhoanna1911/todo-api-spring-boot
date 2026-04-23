package com.imaginemos.todoapi.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTaskItemRequest {

    @Size(min = 1, max = 500, message = "Description must be between 1 and 500 characters")
    private String description;

    private Boolean completed;
}
