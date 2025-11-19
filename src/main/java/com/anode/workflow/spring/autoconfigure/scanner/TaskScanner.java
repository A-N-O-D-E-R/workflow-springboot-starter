package com.anode.workflow.spring.autoconfigure.scanner;

import java.util.*;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.spring.autoconfigure.util.BeanNameUtils;
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
    private void scan() {
        String basePackage = findBasePackage();

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Task.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                registerTaskClass(clazz);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                    "Failed to load @Task class: " + bd.getBeanClassName() +
                    ". Class was found during scanning but cannot be loaded.", e);
            } catch (NoClassDefFoundError e) {
                throw new IllegalStateException(
                    "Failed to load @Task class: " + bd.getBeanClassName() +
                    ". Missing dependency for class.", e);
            }
        }
    }

    private void registerTaskClass(Class<?> clazz) {
        Task annotation = AnnotationUtils.findAnnotation(clazz, Task.class);
        if (annotation == null) return;

        String beanName = annotation.value().isEmpty()
            ? BeanNameUtils.deriveBeanName(clazz)
            : annotation.value();

        // Validate bean name using the comprehensive validator
        BeanNameValidator.validate(beanName, clazz);

        // Check for duplicate bean names
        if (registry.containsKey(beanName)) {
            TaskDescriptor existing = registry.get(beanName);
            throw new IllegalStateException(
                "Duplicate task bean name: '" + beanName + "'. " +
                "Task class '" + clazz.getName() + "' conflicts with existing task. " +
                "Existing task name: '" + existing.taskName() + "'. " +
                "Use @Task(\"uniqueName\") to specify a unique bean name."
            );
        }

        Object bean;
        try {
            bean = context.getBean(beanName);
        } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
            throw new IllegalStateException(
                "Bean '" + beanName + "' referenced by @Task on class '" +
                clazz.getName() + "' not found. " +
                "Ensure the class is annotated with @Component or @Service, or registered as a bean.", e);
        }

        if (!(bean instanceof InvokableTask)) {
            throw new IllegalStateException(
                "Bean '" + beanName + "' is annotated with @Task but does NOT implement InvokableTask"
            );
        }

        String taskName = clazz.getSimpleName().toLowerCase(Locale.ROOT);

        TaskDescriptor descriptor = new TaskDescriptor(
                taskName,
                beanName,
                annotation.order(),
                annotation.userData()
        );

        registry.put(beanName, descriptor);
        taskNameIndex.put(taskName, descriptor);
    }

    /**
     * Automatically finds the root package of the app.
     */
    private String findBasePackage() {
        return context.getEnvironment().getProperty(
                "workflow.scan-base-package",
                "com.anode"
        );
    }




    public record TaskDescriptor(
            String taskName,
            String beanName,
            int order,
            String userData
    ) {}
}
