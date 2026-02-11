package io.metersphere.platform.resolver;

import io.metersphere.platform.client.PhabricatorClient;
import io.metersphere.platform.domain.*;
import io.metersphere.platform.dto.PhabricatorProjectContext;
import io.metersphere.platform.dto.ProjectResolutionResult;
import io.metersphere.platform.enums.ProjectMappingStrategy;
import io.metersphere.platform.response.PhabricatorSearchResponse;

import java.util.*;

/**
 * Resolves Phabricator project mappings using 3-layer strategy
 */
public class PhabricatorProjectResolver {

    private PhabricatorClient client;

    public PhabricatorProjectResolver(PhabricatorClient client) {
        this.client = client;
    }

    /**
     * Main entry point for project resolution
     * Implements 3-layer strategy: Primary + Mandatory Tags + Default Fallback
     */
    public ProjectResolutionResult resolveProject(PhabricatorTask task, PhabricatorProjectConfig config) {
        // Step 1: Apply primary strategy (1 of 4)
        String primaryProjectId = applyPrimaryStrategy(task, config);

        // Step 2: Extract mandatory tags (always)
        List<PlatformCustomFieldItemDTO> tags = extractAllProjectTags(task, config);

        // Step 3: Determine final project (use default if primary fails)
        String finalProjectId = primaryProjectId != null ? primaryProjectId : config.getDefaultProjectId();

        return new ProjectResolutionResult(primaryProjectId, finalProjectId, tags);
    }

    /**
     * Apply one of the 4 primary mapping strategies
     */
    private String applyPrimaryStrategy(PhabricatorTask task, PhabricatorProjectConfig config) {
        if (task == null || task.getProjectPHIDs() == null || task.getProjectPHIDs().isEmpty()) {
            return null;
        }

        ProjectMappingStrategy strategy = config.getPrimaryStrategy();
        if (strategy == null) {
            strategy = ProjectMappingStrategy.SMART_RESOLUTION;
        }

        switch (strategy) {
            case ROOT_ONLY:
                return resolveRootProject(task.getProjectPHIDs());
            case CURRENT_CONTEXT:
                return resolveCurrentContext(task.getProjectPHIDs());
            case HIERARCHICAL:
                return resolveHierarchical(task.getProjectPHIDs());
            case SMART_RESOLUTION:
                return resolveSmart(task.getProjectPHIDs(), config);
            default:
                return resolveSmart(task.getProjectPHIDs(), config);
        }
    }

    /**
     * ROOT_ONLY: Always map to the root/parent project
     */
    private String resolveRootProject(List<String> projectPHIDs) {
        if (projectPHIDs == null || projectPHIDs.isEmpty()) {
            return null;
        }

        // Get the first project's hierarchy and find root
        for (String phid : projectPHIDs) {
            PhabricatorProject project = fetchProject(phid);
            if (project != null) {
                if (project.isRootProject()) {
                    return project.getName();
                }
                // If not root, fetch the parent chain
                String rootPHID = findRootProjectPHID(phid);
                if (rootPHID != null) {
                    PhabricatorProject rootProject = fetchProject(rootPHID);
                    return rootProject != null ? rootProject.getName() : null;
                }
            }
        }

        return null;
    }

    /**
     * CURRENT_CONTEXT: Map to the current project (may be subproject/milestone)
     */
    private String resolveCurrentContext(List<String> projectPHIDs) {
        if (projectPHIDs == null || projectPHIDs.isEmpty()) {
            return null;
        }

        // Use the first/current project
        String currentPHID = projectPHIDs.get(0);
        PhabricatorProject project = fetchProject(currentPHID);
        return project != null ? project.getName() : null;
    }

    /**
     * HIERARCHICAL: Maintain full project path
     */
    private String resolveHierarchical(List<String> projectPHIDs) {
        if (projectPHIDs == null || projectPHIDs.isEmpty()) {
            return null;
        }

        List<String> path = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (String phid : projectPHIDs) {
            buildHierarchyPath(phid, path, visited);
        }

        return String.join("/", path);
    }

    /**
     * SMART_RESOLUTION: Automatically choose best context
     */
    private String resolveSmart(List<String> projectPHIDs, PhabricatorProjectConfig config) {
        if (projectPHIDs == null || projectPHIDs.isEmpty()) {
            return null;
        }

        for (String phid : projectPHIDs) {
            PhabricatorProject project = fetchProject(phid);
            if (project != null) {
                // Priority: milestone > subproject > root project
                if (project.isMilestone()) {
                    return project.getName();
                }
            }
        }

        // Next priority: subproject
        for (String phid : projectPHIDs) {
            PhabricatorProject project = fetchProject(phid);
            if (project != null && project.isSubproject()) {
                return project.getName();
            }
        }

        // Last resort: root project
        return resolveRootProject(projectPHIDs);
    }

    /**
     * Extract all project tags (mandatory feature)
     */
    private List<PlatformCustomFieldItemDTO> extractAllProjectTags(PhabricatorTask task, PhabricatorProjectConfig config) {
        List<PlatformCustomFieldItemDTO> tags = new ArrayList<>();

        if (task == null || task.getProjectPHIDs() == null) {
            return tags;
        }

        String tagFormat = config.getTagFormat();
        if (tagFormat == null || tagFormat.isEmpty()) {
            tagFormat = "[{type}] {name}";
        }

        for (String projectPHID : task.getProjectPHIDs()) {
            PhabricatorProject project = fetchProject(projectPHID);
            if (project != null) {
                PlatformCustomFieldItemDTO tag = createProjectTag(project, tagFormat);
                tags.add(tag);

                // Also add parent projects if configured
                if (config.isIncludeRootTag() && !project.isRootProject()) {
                    addParentTags(project, tags, tagFormat, new HashSet<>());
                }
            }
        }

        return tags;
    }

    private PlatformCustomFieldItemDTO createProjectTag(PhabricatorProject project, String tagFormat) {
        String tagName = tagFormat
            .replace("{type}", project.getProjectType())
            .replace("{name}", project.getName())
            .replace("{phid}", project.getPhid());

        PlatformCustomFieldItemDTO tag = new PlatformCustomFieldItemDTO();
        tag.setId(project.getPhid());
        tag.setName(tagName);
        // Note: value will be set by parent class

        return tag;
    }

    private void addParentTags(PhabricatorProject project, List<PlatformCustomFieldItemDTO> tags,
                               String tagFormat, Set<String> visited) {
        if (project.getParentPHID() == null || visited.contains(project.getParentPHID())) {
            return;
        }

        visited.add(project.getParentPHID());
        PhabricatorProject parent = fetchProject(project.getParentPHID());
        if (parent != null) {
            PlatformCustomFieldItemDTO parentTag = createProjectTag(parent, tagFormat);
            tags.add(parentTag);
            addParentTags(parent, tags, tagFormat, visited);
        }
    }

    private String findRootProjectPHID(String projectPHID) {
        String currentPHID = projectPHID;
        Set<String> visited = new HashSet<>();

        while (currentPHID != null && !visited.contains(currentPHID)) {
            visited.add(currentPHID);
            PhabricatorProject project = fetchProject(currentPHID);
            if (project == null) {
                break;
            }
            if (project.isRootProject()) {
                return currentPHID;
            }
            currentPHID = project.getParentPHID();
        }

        return null;
    }

    private void buildHierarchyPath(String projectPHID, List<String> path, Set<String> visited) {
        if (projectPHID == null || visited.contains(projectPHID)) {
            return;
        }

        visited.add(projectPHID);
        PhabricatorProject project = fetchProject(projectPHID);
        if (project == null) {
            return;
        }

        // First add parents
        if (!project.isRootProject() && project.getParentPHID() != null) {
            buildHierarchyPath(project.getParentPHID(), path, visited);
        }

        // Then add this project
        if (!path.contains(project.getName())) {
            path.add(project.getName());
        }
    }

    /**
     * Fetch project details from cache or API
     */
    private Map<String, PhabricatorProject> projectCache = new HashMap<>();

    @SuppressWarnings("unchecked")
    private PhabricatorProject fetchProject(String projectPHID) {
        if (projectCache.containsKey(projectPHID)) {
            return projectCache.get(projectPHID);
        }

        try {
            Map<String, Object> constraints = new HashMap<>();
            constraints.put("phids", List.of(projectPHID));

            PhabricatorSearchResponse<Map<String, Object>> response = client.searchProjects(constraints);
            if (response.getData() != null && !response.getData().isEmpty()) {
                Map<String, Object> projectData = response.getData().get(projectPHID);
                if (projectData != null) {
                    PhabricatorProject project = parseProject(projectData);
                    projectCache.put(projectPHID, project);
                    return project;
                }
            }
        } catch (Exception e) {
            // Log error but don't fail
            System.err.println("Failed to fetch project " + projectPHID + ": " + e.getMessage());
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private PhabricatorProject parseProject(Map<String, Object> projectData) {
        PhabricatorProject project = new PhabricatorProject();

        project.setPhid((String) projectData.get("phid"));
        project.setId(String.valueOf(projectData.get("id")));

        Map<String, Object> fields = (Map<String, Object>) projectData.get("fields");
        if (fields != null) {
            project.setName((String) fields.get("name"));
            project.setSlug((String) fields.get("slug"));
            project.setSubtype((String) fields.get("subtype"));

            Integer depth = (Integer) fields.get("depth");
            if (depth != null) {
                project.setDepth(depth);
            }

            // Check if milestone
            if (fields.containsKey("milestone")) {
                Object milestone = fields.get("milestone");
                if (milestone != null) {
                    project.setMilestone(milestone.toString());
                }
            }
        }

        // Parse parent from attachments
        Map<String, Object> attachments = (Map<String, Object>) projectData.get("attachments");
        if (attachments != null) {
            Map<String, Object> parentAttachment = (Map<String, Object>) attachments.get("parent");
            if (parentAttachment != null) {
                List<String> parentPHIDs = (List<String>) parentAttachment.get("parentPHIDs");
                if (parentPHIDs != null && !parentPHIDs.isEmpty()) {
                    project.setParentPHID(parentPHIDs.get(0));
                }
            }
        }

        return project;
    }

    /**
     * Get complete project context for a task
     */
    public PhabricatorProjectContext getProjectContext(PhabricatorTask task) {
        PhabricatorProjectContext context = new PhabricatorProjectContext();

        if (task == null || task.getProjectPHIDs() == null) {
            return context;
        }

        Map<String, PhabricatorProject> projectDetails = new HashMap<>();
        List<String> allPHIDs = new ArrayList<>();
        List<String> allNames = new ArrayList<>();

        for (String phid : task.getProjectPHIDs()) {
            PhabricatorProject project = fetchProject(phid);
            if (project != null) {
                projectDetails.put(phid, project);
                allPHIDs.add(phid);
                allNames.add(project.getName());

                // Set current
                if (context.getCurrentProjectPHID() == null) {
                    context.setCurrentProjectPHID(phid);
                    context.setCurrentProjectName(project.getName());
                    context.setCurrentProjectType(project.getProjectType());
                }

                // Find and set root
                if (project.isRootProject()) {
                    context.setRootProjectPHID(phid);
                    context.setRootProjectName(project.getName());
                }
            }
        }

        // If no root found yet, find it
        if (context.getRootProjectPHID() == null && !task.getProjectPHIDs().isEmpty()) {
            String rootPHID = findRootProjectPHID(task.getProjectPHIDs().get(0));
            if (rootPHID != null) {
                PhabricatorProject rootProject = fetchProject(rootPHID);
                if (rootProject != null) {
                    context.setRootProjectPHID(rootPHID);
                    context.setRootProjectName(rootProject.getName());
                    projectDetails.put(rootPHID, rootProject);
                }
            }
        }

        context.setAllProjectPHIDs(allPHIDs);
        context.setAllProjectNames(allNames);
        context.setProjectDetails(projectDetails);

        return context;
    }
}