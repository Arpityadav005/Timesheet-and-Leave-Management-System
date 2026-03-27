package com.tms.ts.service;

import com.tms.ts.dto.ProjectRequest;
import com.tms.ts.dto.ProjectResponse;
import java.util.List;

public interface ProjectService {

    ProjectResponse createProject(ProjectRequest request);

    ProjectResponse updateProject(String id, ProjectRequest request);

    ProjectResponse getProject(String id);

    List<ProjectResponse> getAllActiveProjects();

    void deactivateProject(String id);
}