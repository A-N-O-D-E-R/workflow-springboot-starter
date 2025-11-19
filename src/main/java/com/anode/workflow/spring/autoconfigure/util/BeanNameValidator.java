package com.anode.workflow.spring.autoconfigure.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator for Spring bean names to ensure they follow proper naming conventions.
 *
 * <p>This validator checks bean names against Spring's bean naming conventions and
 * best practices to prevent runtime errors and naming conflicts.
 *
 * <p><b>Valid bean name rules:</b>
 * <ul>
 *   <li>Must not be null or empty (after trimming)</li>
 *   <li>Must start with a letter (a-z, A-Z)</li>
 *   <li>Can contain letters, numbers, underscores (_), hyphens (-), and dots (.)</li>
 *   <li>Cannot end with dot (.) or hyphen (-)</li>
 *   <li>Cannot be a reserved Spring bean name</li>
 *   <li>Should not exceed 255 characters (warning only)</li>
 *   <li>Should not contain consecutive special characters</li>
 *   <li>Should follow camelCase or kebab-case convention</li>
 * </ul>
 *
 * @see <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-definition">Spring Bean Naming</a>
 */
public class BeanNameValidator {

    private static final Logger logger = LoggerFactory.getLogger(BeanNameValidator.class);

    /**
     * Maximum recommended bean name length.
     * Most systems can handle longer names, but this is a practical limit.
     */
    public static final int MAX_RECOMMENDED_LENGTH = 255;

    /**
     * Pattern for valid bean names.
     * Must start with letter, followed by letters, numbers, underscores, hyphens, or dots.
     */
    private static final Pattern VALID_BEAN_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9._-]*$");

    /**
     * Pattern to detect consecutive special characters.
     */
    private static final Pattern CONSECUTIVE_SPECIAL_CHARS = Pattern.compile("[._-]{2,}");

    /**
     * Reserved Spring Framework bean names that should not be used.
     */
    private static final Set<String> RESERVED_BEAN_NAMES = Set.of(
        // Core Spring beans
        "applicationContext",
        "beanFactory",
        "environment",
        "systemProperties",
        "systemEnvironment",
        "messageSource",
        "applicationEventMulticaster",
        "lifecycleProcessor",

        // Spring Boot beans
        "springApplicationArguments",
        "springBootBanner",
        "propertySourcesPlaceholderConfigurer",

        // Common infrastructure beans
        "conversionService",
        "validator",
        "mvcValidator",
        "transactionManager",
        "dataSource",
        "entityManagerFactory",
        "sessionFactory"
    );

    /**
     * Validates a bean name and throws an exception if invalid.
     *
     * @param beanName the bean name to validate
     * @param contextClass the class context for error messages (can be null)
     * @throws IllegalStateException if the bean name is invalid
     */
    public static void validate(String beanName, Class<?> contextClass) {
        ValidationResult result = validateWithResult(beanName, contextClass);
        if (!result.isValid()) {
            throw new IllegalStateException(result.getErrorMessage());
        }

        // Log warnings if any
        result.getWarnings().forEach(warning ->
            logger.warn("Bean name validation warning for '{}': {}", beanName, warning)
        );
    }

    /**
     * Validates a bean name and returns a detailed result.
     *
     * @param beanName the bean name to validate
     * @param contextClass the class context for error messages (can be null)
     * @return validation result with errors and warnings
     */
    public static ValidationResult validateWithResult(String beanName, Class<?> contextClass) {
        ValidationResult result = new ValidationResult();
        String contextInfo = contextClass != null ? " for @Task on class '" + contextClass.getName() + "'" : "";

        // Check for null or empty
        if (beanName == null) {
            result.addError("Bean name cannot be null" + contextInfo + ".");
            return result; // Can't continue validation
        }

        String trimmedName = beanName.trim();

        if (trimmedName.isEmpty()) {
            result.addError("Bean name cannot be empty" + contextInfo + ". Found: '" + beanName + "'");
            return result; // Can't continue validation
        }

        // Check if trimming changed the name (leading/trailing whitespace)
        if (!beanName.equals(trimmedName)) {
            result.addWarning("Bean name has leading or trailing whitespace. " +
                "Trimmed '" + beanName + "' to '" + trimmedName + "'");
        }

        // Check minimum length
        if (trimmedName.length() < 2) {
            result.addWarning("Bean name '" + trimmedName + "' is very short. " +
                "Consider using a more descriptive name for better readability.");
        }

        // Check that it starts with a letter
        char firstChar = trimmedName.charAt(0);
        if (!Character.isLetter(firstChar)) {
            result.addError("Bean name '" + trimmedName + "'" + contextInfo + " must start with a letter. " +
                "Found: '" + firstChar + "' (character code: " + (int)firstChar + ")");
            return result; // Pattern validation will fail anyway
        }

        // Check for valid characters using pattern
        if (!VALID_BEAN_NAME_PATTERN.matcher(trimmedName).matches()) {
            result.addError("Bean name '" + trimmedName + "'" + contextInfo + " contains invalid characters. " +
                "Bean name must start with a letter and contain only letters, numbers, " +
                "underscores (_), hyphens (-), and dots (.). " +
                "Invalid characters: " + findInvalidCharacters(trimmedName));
            return result;
        }

        // Check that it doesn't end with special characters
        char lastChar = trimmedName.charAt(trimmedName.length() - 1);
        if (lastChar == '.' || lastChar == '-') {
            result.addError("Bean name '" + trimmedName + "'" + contextInfo +
                " cannot end with special characters (. or -). Found: '" + lastChar + "'");
        }

        // Check for consecutive special characters
        if (CONSECUTIVE_SPECIAL_CHARS.matcher(trimmedName).find()) {
            result.addWarning("Bean name '" + trimmedName + "' contains consecutive special characters. " +
                "This is valid but may reduce readability. Consider: " +
                suggestAlternativeName(trimmedName));
        }

        // Check for reserved names
        if (RESERVED_BEAN_NAMES.contains(trimmedName)) {
            result.addError("Bean name '" + trimmedName + "'" + contextInfo +
                " is a reserved Spring bean name and cannot be used. " +
                "Choose a different name to avoid conflicts with Spring Framework beans.");
        }

        // Check case-insensitive reserved names (warn only)
        if (RESERVED_BEAN_NAMES.stream().anyMatch(reserved -> reserved.equalsIgnoreCase(trimmedName))) {
            result.addWarning("Bean name '" + trimmedName + "' is similar to a reserved Spring bean name. " +
                "This may cause confusion. Consider using a more distinct name.");
        }

        // Check length
        if (trimmedName.length() > MAX_RECOMMENDED_LENGTH) {
            result.addWarning("Bean name '" + trimmedName + "' is very long (" +
                trimmedName.length() + " characters). " +
                "Consider using a shorter name (recommended max: " + MAX_RECOMMENDED_LENGTH + " characters) " +
                "for better readability and maintainability.");
        }

        // Check naming convention
        if (!followsNamingConvention(trimmedName)) {
            result.addWarning("Bean name '" + trimmedName + "' does not follow standard naming conventions. " +
                "Consider using camelCase (e.g., 'myTaskName') or kebab-case (e.g., 'my-task-name').");
        }

        // Check for numeric suffixes (common anti-pattern)
        if (trimmedName.matches(".*\\d+$") && !trimmedName.matches(".*[a-zA-Z]\\d$")) {
            result.addWarning("Bean name '" + trimmedName + "' ends with multiple digits. " +
                "This may indicate versioning in the name, which is not recommended. " +
                "Consider using a more descriptive name.");
        }

        return result;
    }

    /**
     * Finds invalid characters in a bean name.
     */
    private static String findInvalidCharacters(String beanName) {
        StringBuilder invalid = new StringBuilder();
        for (char c : beanName.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != '.') {
                if (invalid.length() > 0) invalid.append(", ");
                invalid.append("'").append(c).append("'");
                invalid.append(" (code: ").append((int)c).append(")");
            }
        }
        return invalid.length() > 0 ? invalid.toString() : "unknown";
    }

    /**
     * Checks if a bean name follows standard naming conventions.
     */
    private static boolean followsNamingConvention(String name) {
        // Check for camelCase: starts with lowercase, has uppercase letters
        boolean isCamelCase = Character.isLowerCase(name.charAt(0)) &&
            name.chars().anyMatch(Character::isUpperCase);

        // Check for kebab-case: lowercase with hyphens
        boolean isKebabCase = name.equals(name.toLowerCase()) && name.contains("-");

        // Check for snake_case: lowercase with underscores
        boolean isSnakeCase = name.equals(name.toLowerCase()) && name.contains("_");

        // Check for dot notation: contains dots (package-like)
        boolean isDotNotation = name.contains(".");

        return isCamelCase || isKebabCase || isSnakeCase || isDotNotation ||
            name.equals(name.toLowerCase()); // all lowercase is acceptable
    }

    /**
     * Suggests an alternative name by removing consecutive special characters.
     */
    private static String suggestAlternativeName(String name) {
        return name.replaceAll("[._-]{2,}", "-");
    }

    /**
     * Result of bean name validation.
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public String getErrorMessage() {
            if (errors.isEmpty()) return null;
            if (errors.size() == 1) return errors.get(0);

            StringBuilder sb = new StringBuilder("Multiple validation errors:\n");
            for (int i = 0; i < errors.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
            }
            return sb.toString().trim();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ValidationResult{valid=").append(isValid());
            if (!errors.isEmpty()) {
                sb.append(", errors=").append(errors);
            }
            if (!warnings.isEmpty()) {
                sb.append(", warnings=").append(warnings);
            }
            sb.append("}");
            return sb.toString();
        }
    }
}
