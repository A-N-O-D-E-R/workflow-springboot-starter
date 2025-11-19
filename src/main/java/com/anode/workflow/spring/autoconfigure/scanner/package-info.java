/**
 * Classpath scanning for automatic task discovery and registration.
 *
 * <p>This package provides the infrastructure for automatically discovering
 * workflow tasks annotated with {@code @Task} and registering them in the
 * Spring application context.
 *
 * <h2>Main Component</h2>
 * <p>{@link com.anode.workflow.spring.autoconfigure.scanner.TaskScanner} -
 * Scans configured packages for {@code @Task} annotated classes and builds
 * a registry of available tasks.
 *
 * <h2>How It Works</h2>
 * <ol>
 *   <li>During Spring Boot startup, TaskScanner scans configured packages</li>
 *   <li>Finds all classes annotated with {@code @Task}</li>
 *   <li>Validates bean names according to Spring conventions</li>
 *   <li>Registers tasks in an internal registry</li>
 *   <li>Makes tasks available for workflow execution</li>
 * </ol>
 *
 * <h2>Configuration</h2>
 * <p>Packages to scan are configured per engine:
 * <pre>{@code
 * workflow:
 *   engines:
 *     - name: main-engine
 *       scan-packages:
 *         - com.example.tasks
 *         - com.example.workflows
 * }</pre>
 *
 * <h2>Task Registration</h2>
 * <p>Tasks are registered with their bean names:
 * <pre>{@code
 * @Task("processOrder")  // Explicit bean name
 * public class ProcessOrderTask implements InvokableTask {
 *     // ...
 * }
 *
 * @Task  // Bean name derived from class: "processOrderTask"
 * public class ProcessOrderTask implements InvokableTask {
 *     // ...
 * }
 * }</pre>
 *
 * <h2>Bean Name Validation</h2>
 * <p>Bean names must follow these rules:
 * <ul>
 *   <li>Start with a letter (a-z, A-Z)</li>
 *   <li>Contain only letters, numbers, underscores, hyphens, dots</li>
 *   <li>Not end with special characters</li>
 *   <li>Not be reserved Spring bean names</li>
 *   <li>Be unique within the application</li>
 * </ul>
 *
 * <p>Validation is performed by {@link com.anode.workflow.spring.autoconfigure.util.BeanNameValidator}.
 *
 * <h2>Task Lookup</h2>
 * <p>After scanning, tasks can be looked up by name:
 * <pre>{@code
 * @Autowired
 * private TaskScanner scanner;
 *
 * TaskDescriptor task = scanner.getByBeanName("processOrder");
 * }</pre>
 *
 * <h2>Error Handling</h2>
 * <p>Common errors during scanning:
 * <ul>
 *   <li>Invalid bean name - throws {@code IllegalStateException}</li>
 *   <li>Duplicate bean names - Spring context failure</li>
 *   <li>Class loading errors - logged and skipped</li>
 * </ul>
 *
 * @see com.anode.workflow.spring.autoconfigure.annotations.Task
 * @see com.anode.workflow.spring.autoconfigure.util.BeanNameValidator
 * @see org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
 * @since 0.0.1
 */
package com.anode.workflow.spring.autoconfigure.scanner;
