package com.imaginemos.todoapi.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class CreateTaskItemRequest {

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;
}
