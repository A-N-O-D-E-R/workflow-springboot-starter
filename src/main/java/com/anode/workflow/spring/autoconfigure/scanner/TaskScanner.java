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

import jakarta.annotation.PostConstruct;

@Component
public class TaskScanner {

    private final ApplicationContext context;
    private final Map<String, TaskDescriptor> registry = new HashMap<>();
    private final Map<String, TaskDescriptor> taskNameIndex = new HashMap<>();

    public TaskScanner(ApplicationContext context) {
        this.context = context;
    }

    @PostConstruct
    public void init() {
        scan();
    }

    public Map<String, TaskDescriptor> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }

    public TaskDescriptor getByBeanName(String beanName) {
        return registry.get(beanName);
    }

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
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Failed loading @Task class: " + bd.getBeanClassName(), e);
            }
        }
    }

    private void registerTaskClass(Class<?> clazz) {
        Task annotation = AnnotationUtils.findAnnotation(clazz, Task.class);
        if (annotation == null) return;

        String beanName = annotation.value().isEmpty()
            ? deriveBeanName(clazz)
            : annotation.value();

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
     * Derive bean name from class following Spring conventions.
     */
    private String deriveBeanName(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
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
