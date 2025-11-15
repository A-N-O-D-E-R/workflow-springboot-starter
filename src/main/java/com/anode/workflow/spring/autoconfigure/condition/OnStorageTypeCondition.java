package com.anode.workflow.spring.autoconfigure.condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import com.anode.workflow.spring.autoconfigure.properties.JpaProperties.StorageType;
import com.anode.workflow.spring.autoconfigure.properties.WorkflowEnginesProperties;

/**
 * Condition that checks if any workflow engine is configured to use a specific storage type.
 *
 * <p>This reads the workflow.engines configuration and checks if any engine
 * has storage.type matching the requested type.
 */
public class OnStorageTypeCondition implements Condition {

    private static final Logger logger = LoggerFactory.getLogger(OnStorageTypeCondition.class);

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // Get the required storage type from the annotation
        StorageType requiredType = (StorageType) metadata
            .getAnnotationAttributes(ConditionalOnStorageType.class.getName())
            .get("value");

        if (requiredType == null) {
            logger.warn("ConditionalOnStorageType used without specifying storage type");
            return false;
        }

        try {
            // Bind the workflow properties from the environment
            Binder binder = Binder.get(context.getEnvironment());
            WorkflowEnginesProperties properties = binder
                .bind("workflow", WorkflowEnginesProperties.class)
                .orElse(new WorkflowEnginesProperties());

            // Check if any engine uses the required storage type
            boolean hasMatchingEngine = properties.getEngines().stream()
                .anyMatch(engine -> engine.getStorage().getType() == requiredType);

            if (hasMatchingEngine) {
                logger.debug("Storage type {} is configured in at least one engine - bean will be created",
                    requiredType);
            } else {
                logger.debug("Storage type {} is not configured in any engine - bean will be skipped",
                    requiredType);
            }

            return hasMatchingEngine;

        } catch (Exception e) {
            logger.warn("Failed to check storage type condition for {}: {}",
                requiredType, e.getMessage());
            // If we can't determine, default to true to avoid breaking existing setups
            return true;
        }
    }
}
