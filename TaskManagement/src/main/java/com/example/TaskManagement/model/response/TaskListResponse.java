package com.example.TaskManagement.model.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TaskListResponse {
    private List<TaskResponse> tasks = new ArrayList<>();
}
