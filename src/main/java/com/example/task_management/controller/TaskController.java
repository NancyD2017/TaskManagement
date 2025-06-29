package com.example.task_management.controller;

import com.example.task_management.mapper.TaskMapper;
import com.example.task_management.model.entity.Task;
import com.example.task_management.model.request.TaskFilterRequest;
import com.example.task_management.model.request.UpsertPutRequest;
import com.example.task_management.model.request.UpsertTaskRequest;
import com.example.task_management.model.response.TaskListResponse;
import com.example.task_management.model.response.TaskResponse;
import com.example.task_management.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/taskManagement/tasks")
@RequiredArgsConstructor
@Tag(name = "Задачи", description = "Контроллер для управления задачами")
public class TaskController {
    private final TaskService taskService;
    private final TaskMapper taskMapper;

    @Operation(summary = "Получить все задачи", description = "Возвращает список всех задач (только для админов)")
    @ApiResponse(responseCode = "200", description = "Список задач")
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TaskListResponse> getAllTasks() {
        return ResponseEntity.ok(taskMapper.toTaskListResponse(taskService.findAll()));
    }


    @Operation(summary = "Получить задачу по ID", description = "Возвращает задачу по идентификатору (только для админов)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача найдена"),
            @ApiResponse(responseCode = "404", description = "Задача не найдена")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TaskResponse> getById(@PathVariable Long id) {
        Task t = taskService.findById(id);
        return ResponseEntity.ok(taskMapper.taskToResponse(t));
    }


    @Operation(summary = "Фильтр задач", description = "Фильтрует задачи по автору и исполнителю, а также проводит пагинацию")
    @ApiResponse(responseCode = "200", description = "Список задач с фильтром")
    @GetMapping("/filter")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<TaskResponse>> filterBy(@Valid @RequestBody TaskFilterRequest filter) {
        return ResponseEntity.ok(taskMapper.taskListToTaskResponseList(taskService.filterBy(filter)));
    }


    @Operation(summary = "Создать задачу", description = "Добавляет новую задачу (только для админов)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Задача создана"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные")
    })
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody UpsertTaskRequest task) {
        Task t = taskService.save(taskMapper.requestToTask(task), task);
        return ResponseEntity.ok(taskMapper.taskToResponse(t));
    }


    @Operation(summary = "Обновить задачу", description = "Обновляет задачу (только для админов)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Задача обновлена"),
            @ApiResponse(responseCode = "400", description = "Некорректный authorId, assigneeId или taskId")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TaskResponse> updateTask(@Valid @PathVariable Long id, @RequestBody UpsertTaskRequest task) {
        Task t = taskService.update(id, task);
        return ResponseEntity.ok(taskMapper.taskToResponse(t));
    }


    @Operation(summary = "Добавить комментарий к задаче", description = "Добавляет новый комментарий к задаче (админ/пользователь)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Комментарий к задаче добавлен"),
            @ApiResponse(responseCode = "404", description = "Задача не найдена")
    })
    @PutMapping("/addComment/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_USER')")
    public ResponseEntity<TaskResponse> addComment(@PathVariable Long id, @RequestBody UpsertPutRequest comment) {
        Task t = taskService.addComment(id, comment.getRequest());
        return ResponseEntity.ok(taskMapper.taskToResponse(t));
    }


    @Operation(summary = "Изменить статус задачи", description = "Изменяет статус задачи (админ/пользователь)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Статус задачи обновлен"),
            @ApiResponse(responseCode = "400", description = "Некорректный статус"),
            @ApiResponse(responseCode = "404", description = "Задача не найдена")
    })
    @PutMapping("/changeStatus/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_USER')")
    public ResponseEntity<TaskResponse> changeStatus(@PathVariable Long id, @RequestBody UpsertPutRequest status) {
        Task t = taskService.changeStatus(id, status.getRequest());
        return ResponseEntity.ok(taskMapper.taskToResponse(t));
    }


    @Operation(summary = "Удалить задачу", description = "Удаляет задачу по ID (только для админов)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Задача удалена"),
            @ApiResponse(responseCode = "404", description = "Задача не найдена")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(Exception ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<TaskResponse> handleEntityNotFound(Exception ex) {
        return ResponseEntity.notFound().build();
    }
}


