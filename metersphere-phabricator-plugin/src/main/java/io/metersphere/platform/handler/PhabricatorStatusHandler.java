package io.metersphere.platform.handler;

import io.metersphere.platform.domain.PhabricatorProjectConfig;
import io.metersphere.platform.domain.PhabricatorTask;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Handles Phabricator status mapping and closure detection
 */
public class PhabricatorStatusHandler {

    @Setter
    private PhabricatorProjectConfig config;

    public PhabricatorStatusHandler(PhabricatorProjectConfig config) {
        this.config = config;
    }

    /**
     * Check if a Phabricator status means the issue is closed
     */
    public boolean isIssueClosed(String status) {
        if (status == null) {
            return false;
        }
        List<String> closureStatuses = config.getClosureStatuses();
        if (closureStatuses == null || closureStatuses.isEmpty()) {
            // Default closure statuses
            closureStatuses = List.of("resolved", "wontfix", "invalid", "spite", "duplicate");
        }
        return closureStatuses.contains(status.toLowerCase());
    }

    /**
     * Map Phabricator status to MeterSphere status
     */
    public String mapToMeterSphereStatus(String phabricatorStatus) {
        if (phabricatorStatus == null) {
            return "Open";
        }

        // Check custom mapping first
        Map<String, String> statusMapping = config.getStatusMapping();
        if (statusMapping != null && statusMapping.containsKey(phabricatorStatus)) {
            return statusMapping.get(phabricatorStatus);
        }

        // Default mapping
        if (isIssueClosed(phabricatorStatus)) {
            return "Closed";
        }

        // Map common open statuses
        switch (phabricatorStatus.toLowerCase()) {
            case "open":
                return "Open";
            case "in-progress":
            case "in_progress":
                return "In Progress";
            case "todo":
            case "to_do":
                return "To Do";
            default:
                return "Open";
        }
    }

    /**
     * Map MeterSphere status to Phabricator status
     */
    public String mapToPhabricatorStatus(String meterSphereStatus) {
        if (meterSphereStatus == null) {
            return "open";
        }

        // Reverse mapping from custom mappings
        Map<String, String> statusMapping = config.getStatusMapping();
        if (statusMapping != null) {
            for (Map.Entry<String, String> entry : statusMapping.entrySet()) {
                if (entry.getValue().equals(meterSphereStatus)) {
                    return entry.getKey();
                }
            }
        }

        // Default reverse mapping
        switch (meterSphereStatus.toLowerCase()) {
            case "closed":
            case "done":
            case "resolved":
                return "resolved";
            case "in progress":
            case "in-progress":
                return "in-progress";
            case "open":
            case "todo":
            case "to do":
                return "open";
            default:
                return "open";
        }
    }

    /**
     * Check if we should sync closed issues based on configuration
     */
    public boolean shouldSyncClosedIssues() {
        return config.isSyncClosedIssues();
    }

    /**
     * Determine if a task should be skipped based on closure status and config
     */
    public boolean shouldSkipTask(PhabricatorTask task) {
        if (task == null || task.getStatus() == null) {
            return false;
        }

        // Skip if issue is closed and we're not syncing closed issues
        if (isIssueClosed(task.getStatus()) && !shouldSyncClosedIssues()) {
            return true;
        }

        return false;
    }
}