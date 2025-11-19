/**
 * Default implementations of workflow engine interfaces.
 *
 * <p>This package provides no-operation (no-op) default implementations of
 * workflow engine interfaces that are used when the application doesn't
 * provide custom implementations.
 *
 * <h2>Available Implementations</h2>
 * <ul>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.impl.DefaultWorkflowComponentFactory} -
 *       Default factory for creating workflow components from Spring beans</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.impl.NoOpsEventHandler} -
 *       No-operation event handler (does nothing)</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.impl.NoOpsSlaQueueManager} -
 *       No-operation SLA queue manager (does nothing)</li>
 * </ul>
 *
 * <h2>Purpose</h2>
 * <p>These implementations allow the workflow engine to function without
 * requiring users to implement every interface. They provide sensible
 * defaults that can be overridden as needed.
 *
 * <h2>Default Behavior</h2>
 * <ul>
 *   <li><b>DefaultWorkflowComponentFactory:</b> Looks up tasks and routes from Spring's
 *       application context using task names as bean names</li>
 *   <li><b>NoOpsEventHandler:</b> Ignores all workflow events</li>
 *   <li><b>NoOpsSlaQueueManager:</b> No SLA queue management (suitable for simple workflows)</li>
 * </ul>
 *
 * <h2>Customization</h2>
 * <p>To provide custom implementations, define beans that implement the
 * corresponding interfaces:
 *
 * <pre>{@code
 * @Configuration
 * public class MyWorkflowConfig {
 *
 *     @Bean
 *     public EventHandler customEventHandler() {
 *         return new MyEventHandler();
 *     }
 *
 *     @Bean
 *     public SlaQueueManager customSlaManager() {
 *         return new MySlaQueueManager();
 *     }
 * }
 * }</pre>
 *
 * <p>The autoconfiguration will detect custom beans and use them instead
 * of the default implementations.
 *
 * @see com.anode.workflow.service.WorkflowComponantFactory
 * @see com.anode.workflow.service.EventHandler
 * @see com.anode.workflow.service.SlaQueueManager
 * @since 0.0.1
 */
package com.anode.workflow.spring.autoconfigure.impl;
