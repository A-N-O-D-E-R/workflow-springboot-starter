package com.anode.workflow.example.config;

import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.WorkflowComponantFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Factory to resolve workflow components (tasks and routes) from Spring context
 */
@Component
public class WorkflowComponentFactory implements WorkflowComponantFactory {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowComponentFactory.class);

    private final ApplicationContext applicationContext;

    public WorkflowComponentFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object getObject(WorkflowContext workflowContext) {
        String componentName = workflowContext.getCompName();

        try {
            logger.debug("Looking up component: {} of type: {}", componentName, workflowContext.getCompType());

            // Try to get bean by name from Spring context
            if (applicationContext.containsBean(componentName)) {
                // Get the bean prototype and inject the workflow context
                return applicationContext.getBean(componentName, workflowContext);
            }

            logger.error("Component not found in Spring context: {}", componentName);
            throw new IllegalArgumentException("Unknown component: " + componentName);

        } catch (Exception e) {
            logger.error("Failed to create component: {}", componentName, e);
            throw new IllegalArgumentException("Failed to create component: " + componentName, e);
        }
    }
}
