package com.anode.workflow.spring.autoconfigure.scanner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.spring.autoconfigure.runtime.ContextualTask;
import com.anode.workflow.spring.autoconfigure.util.BeanNameValidator;

import jakarta.annotation.PostConstruct;

/**
 * Scans the classpath for classes annotated with {@link Task} and builds a registry.
 *
 * <p>This component automatically discovers all task implementations during application startup
 * and makes them available for workflow execution. Tasks can be looked up either by their
 * Spring bean name or by their configured task name.
 *
 * <p><b>Usage:</b> Tasks are automatically registered when annotated with {@code @Task}:
 * <pre>
 * {@literal @}Task(name = "myTask")
 * public class MyTask implements InvokableTask {
 *     // implementation
 * }
 * </pre>
 *
 * @see Task
 * @see TaskDescriptor
 * @see InvokableTask
 */
@Component
public class TaskScanner {

    private final ApplicationContext context;

    /** Registry of tasks indexed by Spring bean name */
    private final Map<String, TaskDescriptor> registry = new HashMap<>();

    /** Index of tasks by their configured task name for faster lookup */
    private final Map<String, TaskDescriptor> taskNameIndex = new HashMap<>();

    /**
     * Constructs a TaskScanner with the given application context.
     *
     * @param context the Spring application context
     */
    public TaskScanner(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Initializes the scanner by performing classpath scanning for {@code @Task} annotations.
     * This method is automatically called after bean construction.
     */
    @PostConstruct
    public void init() {
        scan();
    }

    /**
     * Returns an unmodifiable view of all registered tasks.
     *
     * @return map of bean names to task descriptors
     */
    public Map<String, TaskDescriptor> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }

    /**
     * Retrieves a task descriptor by its Spring bean name.
     *
     * @param beanName the Spring bean name
     * @return the task descriptor, or null if not found
     */
    public TaskDescriptor getByBeanName(String beanName) {
        return registry.get(beanName);
    }

    /**
     * Retrieves a task descriptor by its configured task name.
     *
     * @param taskName the task name (from {@link Task#name()})
     * @return the task descriptor, or null if not found
     */
    public TaskDescriptor getByTaskName(String taskName) {
        return taskNameIndex.get(taskName);
    }

    /** SCAN AND BUILD REGISTRY **/
    /**
     * Indexes the context's {@code @Task} beans. {@code @Task} is a {@code @Component}, so Spring's
     * own component scanning already found them: no classpath scanning here.
     */
    private void scan() {
        for (Map.Entry<String, Object> entry : context.getBeansWithAnnotation(Task.class).entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            Class<?> clazz = AopUtils.getTargetClass(bean);
            Task annotation = context.findAnnotationOnBean(beanName, Task.class);

            BeanNameValidator.validate(beanName, clazz);
            if (!(bean instanceof InvokableTask) && !(bean instanceof ContextualTask)) {
                throw new IllegalStateException("Bean '" + beanName
                        + "' is annotated with @Task but implements neither InvokableTask nor ContextualTask");
            }

            String taskName = clazz.getSimpleName().toLowerCase(Locale.ROOT);
            TaskDescriptor descriptor =
                    new TaskDescriptor(taskName, beanName, annotation.order(), annotation.userData());
            registry.put(beanName, descriptor);
            taskNameIndex.put(taskName, descriptor);
        }
    }

    public record TaskDescriptor(
            String taskName,
            String beanName,
            int order,
            String userData
    ) {}
}
