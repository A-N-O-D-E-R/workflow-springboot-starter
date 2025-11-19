/**
 * Custom annotations for marking and configuring workflow components.
 *
 * <p>This package provides Spring-based annotations that enable automatic discovery
 * and registration of workflow components.
 *
 * <h2>Available Annotations</h2>
 * <ul>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.annotations.Task} - Marks a class as a workflow task</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.annotations.WorkflowEventHandler} - Marks a workflow event handler</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.annotations.WorkflowComponentFactory} - Marks a workflow component factory</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.annotations.SlaQueueManagerComponent} - Marks an SLA queue manager</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Task(value = "processPayment", order = 1)
 * public class PaymentTask implements InvokableTask {
 *
 *     @Override
 *     public void execute(WorkflowContext context) {
 *         // Process payment logic
 *     }
 * }
 * }</pre>
 *
 * <h2>Task Discovery</h2>
 * <p>Classes annotated with {@code @Task} are automatically discovered during Spring Boot
 * startup via classpath scanning. The scanner looks for these annotations in configured
 * packages and registers them as Spring beans.
 *
 * <h2>Bean Naming</h2>
 * <p>The {@code value} attribute of {@code @Task} specifies the bean name. If not provided,
 * a name is derived from the class name. Bean names must:
 * <ul>
 *   <li>Start with a letter</li>
 *   <li>Contain only letters, numbers, underscores, hyphens, and dots</li>
 *   <li>Not be reserved Spring bean names</li>
 *   <li>Be unique within the application context</li>
 * </ul>
 *
 * @see com.anode.workflow.spring.autoconfigure.scanner.TaskScanner
 * @see com.anode.workflow.spring.autoconfigure.util.BeanNameValidator
 * @since 0.0.1
 */
package com.anode.workflow.spring.autoconfigure.annotations;
