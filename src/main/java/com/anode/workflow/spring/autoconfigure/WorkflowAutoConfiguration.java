package com.anode.workflow.spring.autoconfigure;

import com.anode.tool.service.CommonService;
import com.anode.workflow.WorkflowService;
import com.anode.workflow.service.EventHandler;
import com.anode.workflow.service.SlaQueueManager;
import com.anode.workflow.service.WorkflowComponantFactory;
import com.anode.workflow.service.runtime.RuntimeService;
import com.anode.workflow.spring.autoconfigure.storage.FileStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.JpaStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.MemoryStorageConfiguration;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot auto-configuration for the Workflow Engine.
 *
 * <p>This configuration automatically sets up:
 * <ul>
 *   <li>Storage layer (JPA, Memory, or File-based)</li>
 *   <li>Workflow runtime service</li>
 *   <li>Event handlers</li>
 *   <li>SLA queue manager (optional)</li>
 * </ul>
 *
 * <p>To customize configuration, add properties to application.yml:
 * <pre>
 * workflow:
 *   enabled: true
 *   storage:
 *     type: jpa
 * </pre>
 *
 * <p>To disable auto-configuration:
 * <pre>
 * workflow:
 *   enabled: false
 * </pre>
 *
 * @see WorkflowProperties
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(WorkflowProperties.class)
@Import({
    JpaStorageConfiguration.class,
    MemoryStorageConfiguration.class,
    FileStorageConfiguration.class
})
public class WorkflowAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowAutoConfiguration.class);

    private final WorkflowProperties properties;

    public WorkflowAutoConfiguration(WorkflowProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void logConfiguration() {
        logger.info("Workflow Engine Auto-Configuration enabled");
        logger.info("  Storage Type: {}", properties.getStorage().getType());
        logger.info("  JPA Enabled: {}", properties.getJpa().isEnabled());
        logger.info("  SLA Enabled: {}", properties.getSla().isEnabled());
    }

    /**
     * Provides the WorkflowService singleton instance.
     *
     * @return the workflow service instance
     */
    @Bean
    @ConditionalOnMissingBean
    public WorkflowService workflowService() {
        logger.info("Creating WorkflowService bean");
        return WorkflowService.instance();
    }

    /**
     * Provides the RuntimeService for executing workflows.
     *
     * <p>This requires:
     * <ul>
     *   <li>CommonService (storage layer)</li>
     *   <li>WorkflowComponentFactory</li>
     *   <li>EventHandler (optional)</li>
     *   <li>SlaQueueManager (optional)</li>
     * </ul>
     *
     * @param workflowService the workflow service
     * @param commonService the storage service
     * @param componentFactory the component factory for creating workflow components
     * @param eventHandler the event handler (optional)
     * @param slaQueueManager the SLA queue manager (optional)
     * @return configured runtime service
     */
    @Bean
    @ConditionalOnMissingBean
    public RuntimeService runtimeService(
            WorkflowService workflowService,
            CommonService commonService,
            WorkflowComponantFactory componentFactory,
            EventHandler eventHandler,
            SlaQueueManager slaQueueManager) {

        logger.info("Creating RuntimeService bean");
        logger.debug("  CommonService: {}", commonService.getClass().getSimpleName());
        logger.debug("  ComponentFactory: {}", componentFactory.getClass().getSimpleName());
        logger.debug("  EventHandler: {}", eventHandler != null ? eventHandler.getClass().getSimpleName() : "none");
        logger.debug("  SlaQueueManager: {}", slaQueueManager != null ? slaQueueManager.getClass().getSimpleName() : "none");

        return workflowService.getRunTimeService(
            commonService,
            componentFactory,
            eventHandler,
            slaQueueManager
        );
    }

    /**
     * Provides a default no-op event handler if none is configured.
     *
     * <p>Override this by providing your own EventHandler bean.
     *
     * @return default event handler
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "workflow.event-handler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public EventHandler eventHandler() {
        logger.info("Creating default EventHandler bean (no-op)");
        return new DefaultEventHandler();
    }

    /**
     * Provides a default no-op SLA queue manager if SLA is enabled but no custom implementation exists.
     *
     * <p>Override this by providing your own SlaQueueManager bean.
     *
     * @return default SLA queue manager
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "workflow.sla", name = "enabled", havingValue = "true")
    public SlaQueueManager slaQueueManager() {
        logger.info("Creating default SlaQueueManager bean (no-op)");
        return new DefaultSlaQueueManager();
    }

    /**
     * Default no-op event handler implementation.
     */
    private static class DefaultEventHandler implements EventHandler {
        @Override
        public void invoke(com.anode.workflow.entities.events.EventType event,
                          com.anode.workflow.entities.workflows.WorkflowContext workflowContext) {
            // No-op - default implementation does nothing
            // Override this bean to provide custom event handling
        }
    }

    /**
     * Default no-op SLA queue manager implementation.
     */
    private static class DefaultSlaQueueManager implements SlaQueueManager {
        @Override
        public void enqueue(com.anode.workflow.entities.workflows.WorkflowContext pc,
                           java.util.List<com.anode.workflow.entities.sla.Milestone> milestones) {
            // No-op
        }

        @Override
        public void dequeue(com.anode.workflow.entities.workflows.WorkflowContext pc, String wb) {
            // No-op
        }

        @Override
        public void dequeueAll(com.anode.workflow.entities.workflows.WorkflowContext pc) {
            // No-op
        }
    }
}
