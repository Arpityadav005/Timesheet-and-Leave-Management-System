package com.tms.ts.controller;

import com.tms.ts.dto.ProjectRequest;
import com.tms.ts.dto.ProjectResponse;
import com.tms.ts.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Project", description = "Project management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {
    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Create a new project") 
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request) {
        log.info("Creating project code={} name={}", request.getCode(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a project")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectResponse> updateProject(
            @Parameter(description = "Project ID", example = "PRJ-0FC7A60F")
            @PathVariable("id") String id,
            @Valid @RequestBody ProjectRequest request) {
        log.info("Updating project id={} code={}", id, request.getCode());
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a project by id")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ProjectResponse> getProject(
            @Parameter(description = "Project ID", example = "PRJ-0FC7A60F")
            @PathVariable("id") String id) {
        log.info("Fetching project id={}", id);
        return ResponseEntity.ok(projectService.getProject(id));
    }

    @GetMapping
    @Operation(summary = "Get all active projects")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ProjectResponse>> getAllActiveProjects() {
        log.info("Fetching all active projects");
        return ResponseEntity.ok(projectService.getAllActiveProjects());
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a project")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateProject(
            @Parameter(description = "Project ID", example = "PRJ-0FC7A60F")
            @PathVariable("id") String id) {
        log.info("Deactivating project id={}", id);
        projectService.deactivateProject(id);
        return ResponseEntity.noContent().build();
    }
}
