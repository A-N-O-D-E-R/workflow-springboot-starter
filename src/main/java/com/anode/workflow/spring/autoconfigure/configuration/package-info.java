/**
 * Spring Boot autoconfiguration classes for workflow engine setup.
 *
 * <p>This package contains the main autoconfiguration logic that automatically
 * configures workflow engines, runtime services, and related components when
 * the starter is included in a Spring Boot application.
 *
 * <h2>Main Configuration Class</h2>
 * <p>{@link com.anode.workflow.spring.autoconfigure.configuration.WorkflowAutoConfiguration}
 * is the primary autoconfiguration class that:
 * <ul>
 *   <li>Creates RuntimeService beans for each configured engine</li>
 *   <li>Sets up WorkflowService with all configured engines</li>
 *   <li>Provides default implementations for workflow components</li>
 *   <li>Integrates with storage configurations</li>
 * </ul>
 *
 * <h2>Configuration Properties</h2>
 * <p>Configuration is driven by {@code workflow.*} properties defined in
 * {@code application.yml} or {@code application.properties}:
 *
 * <pre>{@code
 * workflow:
 *   engines:
 *     - name: main-engine
 *       storage:
 *         type: jpa
 *       scan-packages:
 *         - com.example.tasks
 *     - name: secondary-engine
 *       storage:
 *         type: memory
 * }</pre>
 *
 * <h2>Bean Creation</h2>
 * <p>The autoconfiguration creates the following beans:
 * <ul>
 *   <li>{@code RuntimeService} - One per configured engine</li>
 *   <li>{@code WorkflowService} - Main service managing all engines</li>
 *   <li>{@code WorkflowEngine} - Facade for workflow execution</li>
 *   <li>{@code FluentWorkflowBuilderFactory} - Factory for fluent builders</li>
 *   <li>Default components (if not provided by user)</li>
 * </ul>
 *
 * <h2>Customization</h2>
 * <p>Default beans can be overridden by providing custom implementations:
 * <pre>{@code
 * @Bean
 * public WorkflowComponantFactory customFactory() {
 *     return new MyCustomFactory();
 * }
 * }</pre>
 *
 * <h2>Load Order</h2>
 * <p>This configuration loads after storage configurations to ensure
 * storage beans are available when creating RuntimeServices.
 *
 * @see com.anode.workflow.spring.autoconfigure.properties.WorkflowEnginesProperties
 * @see com.anode.workflow.spring.autoconfigure.storage
 * @since 0.0.1
 */
package com.anode.workflow.spring.autoconfigure.configuration;
