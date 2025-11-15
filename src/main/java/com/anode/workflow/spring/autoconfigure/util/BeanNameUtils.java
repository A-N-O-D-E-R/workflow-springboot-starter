package com.anode.workflow.spring.autoconfigure.util;

/**
 * Utility class for bean name operations.
 */
public final class BeanNameUtils {

    private BeanNameUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Derives a bean name from a class following Spring conventions.
     * Converts the simple class name to camelCase.
     *
     * <p>Examples:
     * <ul>
     *   <li>MyTask → myTask</li>
     *   <li>WorkflowEngine → workflowEngine</li>
     *   <li>SLAQueueManager → sLAQueueManager</li>
     * </ul>
     *
     * @param clazz the class to derive the bean name from
     * @return the bean name in camelCase
     * @throws IllegalArgumentException if class is null or has empty simple name
     */
    public static String deriveBeanName(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }

        String simpleName = clazz.getSimpleName();
        if (simpleName.isEmpty()) {
            throw new IllegalArgumentException("Class simple name cannot be empty");
        }

        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
}
