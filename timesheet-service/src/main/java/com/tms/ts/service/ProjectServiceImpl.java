package com.tms.ts.service;

import com.tms.common.exception.ResourceAlreadyExistsException;
import com.tms.common.exception.ResourceNotFoundException;
import com.tms.common.util.IdGeneratorUtil;
import com.tms.ts.dto.ProjectRequest;
import com.tms.ts.dto.ProjectResponse;
import com.tms.ts.entity.Project;
import com.tms.ts.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {
    private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);

    private final ProjectRepository projectRepository;
    private final IdGeneratorUtil idGeneratorUtil;

    public ProjectServiceImpl(ProjectRepository projectRepository,
                               IdGeneratorUtil idGeneratorUtil) {
        this.projectRepository = projectRepository;
        this.idGeneratorUtil = idGeneratorUtil;
    }

    @Override
    public ProjectResponse createProject(ProjectRequest request) {
        log.info("Creating project code={} name={}", request.getCode(), request.getName());

        if (projectRepository.existsByCode(request.getCode())) {
            log.warn("Project creation rejected because code already exists: {}", request.getCode());
            throw new ResourceAlreadyExistsException("Project code already exists");
        }

        Project project = new Project();
        project.setId(idGeneratorUtil.generateId("PRJ"));
        project.setCode(request.getCode());
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setActive(true);

        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully projectId={} code={}", savedProject.getId(), savedProject.getCode());

        return mapToProjectResponse(savedProject);
    }

    @Override
    public ProjectResponse updateProject(String id, ProjectRequest request) {
        log.info("Updating project id={} code={}", id, request.getCode());

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getCode().equals(request.getCode())
                && projectRepository.existsByCode(request.getCode())) {
            log.warn("Project update rejected for id={} because code already exists: {}", id, request.getCode());
            throw new ResourceAlreadyExistsException("Project code already exists");
        }

        project.setCode(request.getCode());
        project.setName(request.getName());
        project.setDescription(request.getDescription());

        Project savedProject = projectRepository.save(project);
        log.info("Project updated successfully projectId={}", savedProject.getId());

        return mapToProjectResponse(savedProject);
    }

    @Override
    public ProjectResponse getProject(String id) {
        log.debug("Loading project id={}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return mapToProjectResponse(project);
    }

    @Override
    public List<ProjectResponse> getAllActiveProjects() {
        log.debug("Loading all active projects");
        return projectRepository.findByActiveTrue()
                .stream()
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deactivateProject(String id) {
        log.info("Deactivating project id={}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        project.setActive(false);
        projectRepository.save(project);
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setCode(project.getCode());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setActive(project.isActive());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        return response;
    }
}
