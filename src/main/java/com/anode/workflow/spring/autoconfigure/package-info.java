/**
 * Spring Boot autoconfiguration for OpenEvolve Workflow Engine.
 *
 * <p>This package provides seamless integration of the OpenEvolve Workflow Engine into Spring Boot
 * applications with zero-configuration defaults and flexible customization options.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Automatic workflow engine configuration via Spring Boot autoconfiguration</li>
 *   <li>Support for multiple storage backends (Memory, JPA, File)</li>
 *   <li>Automatic task discovery using {@code @Task} annotation</li>
 *   <li>Fluent API for building and executing workflows</li>
 *   <li>Thread-safe concurrent operations with per-resource locking</li>
 *   <li>Comprehensive input validation and error handling</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // 1. Add dependency to pom.xml
 * <dependency>
 *     <groupId>com.anode</groupId>
 *     <artifactId>workflow-spring-boot-starter</artifactId>
 *     <version>0.0.1</version>
 * </dependency>
 *
 * // 2. Configure in application.yml
 * workflow:
 *   engines:
 *     - name: main-engine
 *       storage:
 *         type: memory
 *
 * // 3. Define tasks
 * @Task("myTask")
 * public class MyTask implements InvokableTask {
 *     public void execute(WorkflowContext context) {
 *         // Task logic
 *     }
 * }
 *
 * // 4. Execute workflows
 * @Autowired
 * private WorkflowEngine engine;
 *
 * WorkflowContext ctx = engine.startWorkflow(
 *     "case-123",
 *     Arrays.asList("myTask", "anotherTask"),
 *     Map.of("key", "value")
 * );
 * }</pre>
 *
 * <h2>Package Organization</h2>
 * <ul>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.annotations} - Custom annotations for workflow components</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.condition} - Custom Spring conditional logic</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.configuration} - Spring Boot autoconfiguration classes</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.impl} - Default implementations of workflow interfaces</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.properties} - Configuration properties classes</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.runtime} - Workflow execution runtime components</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.scanner} - Classpath scanning for task discovery</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.storage} - Storage backend implementations</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.util} - Utility classes</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>The starter can be configured via {@code application.yml} or {@code application.properties}:
 *
 * <pre>{@code
 * workflow:
 *   engines:
 *     - name: engine1
 *       storage:
 *         type: jpa  # or memory, file
 *       scan-packages:
 *         - com.example.tasks
 *     - name: engine2
 *       storage:
 *         type: memory
 * }</pre>
 *
 * @since 0.0.1
 * @version 0.0.1
 */
package com.anode.workflow.spring.autoconfigure;
