package org.acme.employeescheduling.config;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing dynamic constraint configurations with smart defaults.
 * Works out-of-the-box with no configuration needed, but allows runtime customization.
 * 
 * Key Features:
 * - Zero-config defaults that match current hardcoded behavior
 * - Runtime weight/enable adjustments without code changes
 * - Thread-safe for multi-user scenarios
 * - Maintains backward compatibility
 */
@ApplicationScoped
public class ConstraintConfigurationService {

    /**
     * Configuration for a single constraint
     */
    public static class ConstraintConfig {
        private boolean enabled;
        private int weight;
        private String type; // "hard" or "soft"
        private String description;

        public ConstraintConfig() {}

        public ConstraintConfig(boolean enabled, int weight, String type, String description) {
            this.enabled = enabled;
            this.weight = weight;
            this.type = type;
            this.description = description;
        }

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getWeight() { return weight; }
        public void setWeight(int weight) { this.weight = weight; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // Thread-safe storage for constraint configurations
    private final Map<String, ConstraintConfig> constraintConfigs = new ConcurrentHashMap<>();

    public ConstraintConfigurationService() {
        initializeDefaultConfigurations();
    }

    /**
     * Initialize production-ready defaults that match current constraint behavior.
     * These defaults ensure /schedule-view works immediately without any setup.
     */
    private void initializeDefaultConfigurations() {
        // Core scheduling constraints (essential for valid schedules)
        constraintConfigs.put("overlapping_shift", 
            new ConstraintConfig(true, 100, "hard", "Prevent employee working overlapping shifts"));
        constraintConfigs.put("insufficient_rest_after_night_shift", 
            new ConstraintConfig(true, 50, "hard", "24+ hour rest after night shifts"));
        constraintConfigs.put("consecutive_night_shifts", 
            new ConstraintConfig(true, 10, "hard", "No back-to-back night shifts"));
        constraintConfigs.put("work_on_official_leave_day", 
            new ConstraintConfig(true, 100, "hard", "Respect official leave days"));
        constraintConfigs.put("work_on_excluded_day", 
            new ConstraintConfig(true, 20, "hard", "Avoid excluded days"));
        constraintConfigs.put("last_day_shift_employees_need_proper_rest", 
            new ConstraintConfig(true, 30, "hard", "Special rest rules for employees finishing Aug 31st night shift"));

        // Quality constraints (improve schedule quality but not mandatory)
        constraintConfigs.put("prefer_selected_days", 
            new ConstraintConfig(true, 5, "soft", "Assign employees to their preferred days"));
        
        // Future extensibility - disabled by default but ready to enable
        constraintConfigs.put("balance_workload", 
            new ConstraintConfig(false, 3, "soft", "Distribute shifts evenly across employees"));
        constraintConfigs.put("minimize_workplace_changes", 
            new ConstraintConfig(false, 2, "soft", "Keep employees at consistent workplaces"));
        constraintConfigs.put("work_group_specialization", 
            new ConstraintConfig(false, 4, "soft", "Match employee specialties to shift requirements"));
    }

    /**
     * Get configuration for a specific constraint.
     * Returns sensible defaults for unknown constraints to prevent runtime errors.
     */
    public ConstraintConfig getConstraintConfig(String constraintId) {
        ConstraintConfig config = constraintConfigs.get(constraintId);
        if (config != null) {
            return config;
        }
        
        // Fallback defaults for any constraint not explicitly configured
        // Assumes most unknown constraints should be enabled with moderate weight
        return new ConstraintConfig(true, 1, "soft", "Auto-generated default for " + constraintId);
    }

    /**
     * Update configuration for a specific constraint
     */
    public void updateConstraintConfig(String constraintId, ConstraintConfig config) {
        constraintConfigs.put(constraintId, config);
    }

    /**
     * Update multiple constraint configurations at once
     */
    public void updateConstraintConfigs(Map<String, ConstraintConfig> updates) {
        constraintConfigs.putAll(updates);
    }

    /**
     * Get all current constraint configurations
     */
    public Map<String, ConstraintConfig> getAllConfigurations() {
        return new ConcurrentHashMap<>(constraintConfigs);
    }

    /**
     * Check if a constraint is enabled
     */
    public boolean isConstraintEnabled(String constraintId) {
        ConstraintConfig config = getConstraintConfig(constraintId);
        return config.isEnabled();
    }

    /**
     * Get effective weight for a constraint (returns 0 if disabled)
     * This is the method constraints will call to get their runtime weight.
     */
    public int getEffectiveWeight(String constraintId) {
        ConstraintConfig config = getConstraintConfig(constraintId);
        return config.isEnabled() ? config.getWeight() : 0;
    }

    /**
     * Reset all configurations to smart defaults
     */
    public void resetToDefaults() {
        constraintConfigs.clear();
        initializeDefaultConfigurations();
    }

    /**
     * Get a summary of active constraints for debugging/monitoring
     */
    public Map<String, String> getActiveConstraintsSummary() {
        Map<String, String> summary = new ConcurrentHashMap<>();
        for (Map.Entry<String, ConstraintConfig> entry : constraintConfigs.entrySet()) {
            ConstraintConfig config = entry.getValue();
            if (config.isEnabled()) {
                summary.put(entry.getKey(), 
                    String.format("%s (weight: %d)", config.getType().toUpperCase(), config.getWeight()));
            }
        }
        return summary;
    }
}