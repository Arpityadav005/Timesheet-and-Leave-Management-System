package com.tms.ts.controller;

import com.tms.ts.dto.ProjectRequest;
import com.tms.ts.dto.ProjectResponse;
import org.junit.jupiter.api.Assertions;
import com.tms.ts.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectController projectController;

    private ProjectResponse projectResponse;
    private ProjectRequest projectRequest;

    @BeforeEach
    void setUp() {
        projectResponse = new ProjectResponse();
        projectResponse.setId("PRJ-101");
        projectResponse.setCode("TMS001");
        projectResponse.setName("TMS Dashboard");
        projectResponse.setActive(true);

        projectRequest = new ProjectRequest();
        projectRequest.setCode("TMS001");
        projectRequest.setName("TMS Dashboard");
    }

    @Test
    @DisplayName("POST /projects - Should create project")
    void createProject_Success() {
        when(projectService.createProject(any(ProjectRequest.class))).thenReturn(projectResponse);

        ResponseEntity<ProjectResponse> response = projectController.createProject(projectRequest);

        Assertions.assertEquals(201, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("PRJ-101", response.getBody().getId());
        Assertions.assertEquals("TMS Dashboard", response.getBody().getName());
    }

    @Test
    @DisplayName("PUT /projects/{id} - Should update project")
    void updateProject_Success() {
        projectRequest.setName("Updated Dashboard");
        projectResponse.setName("Updated Dashboard");

        when(projectService.updateProject(org.mockito.ArgumentMatchers.eq("PRJ-101"), any(ProjectRequest.class))).thenReturn(projectResponse);

        ResponseEntity<ProjectResponse> response = projectController.updateProject("PRJ-101", projectRequest);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("Updated Dashboard", response.getBody().getName());
    }

    @Test
    @DisplayName("GET /projects/{id} - Should fetch project")
    void getProject_Success() {
        when(projectService.getProject("PRJ-101")).thenReturn(projectResponse);

        ResponseEntity<ProjectResponse> response = projectController.getProject("PRJ-101");

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("PRJ-101", response.getBody().getId());
    }

    @Test
    @DisplayName("GET /projects - Should fetch all active projects")
    void getAllActiveProjects_Success() {
        when(projectService.getAllActiveProjects()).thenReturn(List.of(projectResponse));

        ResponseEntity<List<ProjectResponse>> response = projectController.getAllActiveProjects();

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(1, response.getBody().size());
        Assertions.assertEquals("PRJ-101", response.getBody().get(0).getId());
    }

    @Test
    @DisplayName("PATCH /projects/{id}/deactivate - Should deactivate project")
    void deactivateProject_Success() {
        doNothing().when(projectService).deactivateProject("PRJ-101");

        ResponseEntity<Void> response = projectController.deactivateProject("PRJ-101");

        Assertions.assertEquals(204, response.getStatusCode().value());
    }

    @Test
    @DisplayName("POST /projects - Should allow empty request object at controller level")
    void createProject_InvalidData() {
        ProjectRequest invalidRequest = new ProjectRequest();
        when(projectService.createProject(any(ProjectRequest.class))).thenReturn(projectResponse);

        ResponseEntity<ProjectResponse> response = projectController.createProject(invalidRequest);

        Assertions.assertEquals(HttpStatusCode.valueOf(201), response.getStatusCode());
    }
}
