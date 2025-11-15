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

        // Anonymous classes have an empty simpleName → must be handled
        if (simpleName == null || simpleName.isEmpty()) {
            return deriveFromAnonymousClass(clazz);
        }
        
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }


    private static String deriveFromAnonymousClass(Class<?> clazz) {
        // Fallback: use enclosing class + "Anonymous"
        Class<?> enclosing = clazz.getEnclosingClass();

        if (enclosing != null) {
            String base = enclosing.getSimpleName();
            return Character.toLowerCase(base.charAt(0)) + base.substring(1) + "Anonymous";
        }

        // No enclosing class → last part of the class name
        String name = clazz.getName();
        String cleaned = name.substring(name.lastIndexOf('.') + 1)
                            .replaceAll("[^a-zA-Z0-9]", "");

        return cleaned.isEmpty() ? "anonymousBean" : cleaned;
    }
}
