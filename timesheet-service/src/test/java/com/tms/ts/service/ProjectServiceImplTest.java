package com.tms.ts.service;

import com.tms.common.exception.ResourceAlreadyExistsException;
import com.tms.common.exception.ResourceNotFoundException;
import com.tms.common.util.IdGeneratorUtil;
import com.tms.ts.dto.ProjectRequest;
import com.tms.ts.dto.ProjectResponse;
import com.tms.ts.entity.Project;
import com.tms.ts.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private IdGeneratorUtil idGeneratorUtil;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Project testProject;
    private ProjectRequest projectRequest;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setId("PRJ-101");
        testProject.setCode("TMS-001");
        testProject.setName("TMS Dashboard");
        testProject.setDescription("Main project dashboard");
        testProject.setActive(true);

        projectRequest = new ProjectRequest();
        projectRequest.setCode("TMS-001");
        projectRequest.setName("TMS Dashboard");
        projectRequest.setDescription("Main project dashboard");
    }

    @Test
    @DisplayName("Should successfully create a new project")
    void createProject_Success() {
        when(projectRepository.existsByCode("TMS-001")).thenReturn(false);
        when(idGeneratorUtil.generateId("PRJ")).thenReturn("PRJ-101");
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        ProjectResponse response = projectService.createProject(projectRequest);

        assertNotNull(response);
        assertEquals("PRJ-101", response.getId());
        assertEquals("TMS-001", response.getCode());
        assertTrue(response.isActive());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("Should throw exception if project code already exists")
    void createProject_DuplicateCode() {
        when(projectRepository.existsByCode("TMS-001")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, 
                () -> projectService.createProject(projectRequest));
        
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("Should successfully update an existing project")
    void updateProject_Success() {
        when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        ProjectRequest updateRequest = new ProjectRequest();
        updateRequest.setCode("TMS-001");
        updateRequest.setName("Updated Project");

        ProjectResponse response = projectService.updateProject("PRJ-101", updateRequest);

        assertNotNull(response);
        assertEquals("Updated Project", response.getName());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("Should return project when ID exists")
    void getProject_Success() {
        when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));

        ProjectResponse response = projectService.getProject("PRJ-101");

        assertNotNull(response);
        assertEquals("PRJ-101", response.getId());
        assertEquals("TMS Dashboard", response.getName());
    }

    @Test
    @DisplayName("Should deactivate project (set active = false)")
    void deactivateProject_Success() {
        when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
        // Using answer since we don't return from void deactivate method but we assert repository save was called
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        projectService.deactivateProject("PRJ-101");

        assertFalse(testProject.isActive());
        verify(projectRepository).save(testProject);
    }
}
