package com.anode.workflow.spring.autoconfigure.storage;

import com.anode.tool.service.CommonService;
import com.anode.workflow.spring.autoconfigure.condition.ConditionalOnStorageType;
import com.anode.workflow.spring.autoconfigure.properties.JpaProperties.StorageType;
import com.anode.workflow.spring.autoconfigure.properties.WorkflowEnginesProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * File-based JSON storage configuration for workflow engine.
 *
 * <p>This bean is only created when at least one engine is configured to use file storage:
 * <pre>
 * workflow:
 *   engines:
 *     - name: my-engine
 *       storage:
 *         type: file
 *         file-path: ./workflow-data
 * </pre>
 *
 * <p><b>WARNING:</b> This is suitable for development and small-scale testing only.
 * For production use, prefer JPA storage.
 */
@Configuration
@ConditionalOnStorageType(StorageType.FILE)
public class FileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageConfiguration.class);

    /**
     * Creates a file-based CommonService using the configured file path.
     *
     * <p>The file path is read from the first engine configured with FILE storage type.
     * If no file path is explicitly configured, defaults to "./workflow-data".
     *
     * @param enginesProperties the workflow engines configuration
     * @return file-based common service implementation
     */
    @Bean
    @ConditionalOnMissingBean(name = "fileCommonService")
    public CommonService fileCommonService(WorkflowEnginesProperties enginesProperties) {
        // Find the first engine using FILE storage and get its file path
        String filePath = enginesProperties.getEngines().stream()
            .filter(engine -> engine.getStorage().getType() == StorageType.FILE)
            .map(engine -> engine.getStorage().getFilePath())
            .findFirst()
            .orElse("./workflow-data"); // Default fallback

        logger.warn("Configuring FILE-BASED storage for workflow engine");
        logger.warn("  Storage path: {}", filePath);
        logger.warn("  ⚠️  Use only for development and testing");

        // Create directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory: " + filePath, e);
        }

        return new FileCommonService(filePath);
    }

    /**
     * File-based implementation of CommonService using JSON serialization.
     *
     * <p>Each entity is stored as a separate JSON file.
     */
    private static class FileCommonService implements CommonService {
        private final String basePath;
        private final ObjectMapper objectMapper;
        private final Map<String, Long> counters = new HashMap<>();

        public FileCommonService(String basePath) {
            this.basePath = basePath;
            this.objectMapper = new ObjectMapper();
        }

        /**
         * Get the file for the given ID with path traversal protection.
         *
         * @param id the entity ID
         * @return the file for the given ID
         * @throws SecurityException if path traversal is detected
         * @throws IllegalStateException if path validation fails
         */
        private File getFile(Serializable id) {
            // Sanitize the ID to prevent path traversal
            String sanitizedId = sanitizeId(id.toString());
            File file = new File(basePath, sanitizedId + ".json");

            // Validate the file is within basePath (prevent path traversal)
            try {
                String canonicalBase = new File(basePath).getCanonicalPath();
                String canonicalFile = file.getCanonicalPath();
                if (!canonicalFile.startsWith(canonicalBase + File.separator) &&
                    !canonicalFile.equals(canonicalBase)) {
                    throw new SecurityException(
                        String.format("Path traversal attempt detected for ID: %s (resolved to: %s)",
                                    id, canonicalFile)
                    );
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to validate file path for ID: " + id, e);
            }

            return file;
        }

        /**
         * Sanitize an ID string to prevent path traversal attacks.
         * Replaces path separators and parent directory references with underscores.
         *
         * @param id the ID to sanitize
         * @return the sanitized ID
         */
        private String sanitizeId(String id) {
            // Remove or replace dangerous characters:
            // - Forward and back slashes: / \
            // - Colon (Windows drive): :
            // - Wildcards: * ?
            // - Quotes: " <> (Windows reserved)
            // - Pipe: | (Windows reserved)
            // - Null bytes: \x00
            // - Parent directory references: ..
            // Replace each dangerous character or pattern with an underscore
            return id.replaceAll("[/\\\\:*?\"<>|\\x00]|\\.\\.+", "_");
        }

        @Override
        public synchronized void save(Serializable id, Object object) {
            File file = getFile(id);
            if (!file.exists()) {
                write(file, object);
            }
        }

        @Override
        public synchronized void update(Serializable id, Object object) {
            File file = getFile(id);
            if (file.exists()) {
                write(file, object);
            }
        }

        @Override
        public synchronized void saveOrUpdate(Serializable id, Object object) {
            write(getFile(id), object);
        }

        @Override
        public synchronized void delete(Serializable id) {
            File file = getFile(id);
            if (file.exists()) {
                if (!file.delete()) {
                    throw new RuntimeException("Failed to delete file: " + file.getAbsolutePath());
                }
            }
        }

        @Override
        public <T> T get(Class<T> objectClass, Serializable id) {
            File file = getFile(id);
            if (!file.exists()) {
                return null;
            }
            return read(file, objectClass);
        }

        @Override
        public <T> T getLocked(Class<T> objectClass, Serializable id) {
            // File-based storage doesn't support true locking
            // This is a simplified implementation
            return get(objectClass, id);
        }

        @Override
        public void saveCollection(Collection objects) {
            for (Object obj : objects) {
                save(UUID.randomUUID().toString(), obj);
            }
        }

        @Override
        public void saveOrUpdateCollection(Collection objects) {
            for (Object obj : objects) {
                saveOrUpdate(UUID.randomUUID().toString(), obj);
            }
        }

        @Override
        public <T> List<T> getAll(Class<T> type) {
            File dir = new File(basePath);
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));

            if (files == null) {
                return Collections.emptyList();
            }

            List<T> results = new ArrayList<>();
            for (File file : files) {
                // Attempt to read and deserialize each file
                // Files that can't be deserialized to the target type are silently skipped
                T obj = read(file, type);
                if (obj != null) {
                    results.add(obj);
                }
            }
            return results;
        }

        @Override
        public <T> T getUniqueItem(Class<T> type, String uniqueKeyName, String uniqueKeyValue) {
            return getAll(type).stream()
                .filter(obj -> matchesUniqueKey(obj, uniqueKeyName, uniqueKeyValue))
                .findFirst()
                .orElse(null);
        }

        /**
         * Check if an object's field matches the given value using reflection.
         *
         * @param obj the object to check
         * @param fieldName the field name to inspect
         * @param value the expected value
         * @return true if the field exists and matches the value, false otherwise
         */
        private boolean matchesUniqueKey(Object obj, String fieldName, String value) {
            try {
                java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object fieldValue = field.get(obj);
                return value.equals(fieldValue);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // Field doesn't exist or isn't accessible
                logger.debug("Failed to access field '{}' on {}: {}",
                           fieldName, obj.getClass().getSimpleName(), e.getMessage());
                return false;
            }
        }

        @Override
        public synchronized long incrCounter(String counterName) {
            Long value = counters.getOrDefault(counterName, 0L);
            value++;
            counters.put(counterName, value);
            return value;
        }

        @Override
        public Map<Serializable, Serializable> makeClone(Object object, com.anode.tool.service.IdFactory idFactory) {
            throw new UnsupportedOperationException("makeClone not implemented in file storage");
        }

        @Override
        public Serializable getMinimalId(Comparator<Serializable> comparator) {
            throw new UnsupportedOperationException("getMinimalId not implemented in file storage");
        }

        private void write(File file, Object object) {
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, object);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file: " + file, e);
            }
        }

        private <T> T read(File file, Class<T> clazz) {
            try {
                return objectMapper.readValue(file, clazz);
            } catch (IOException e) {
                // Log at debug level - this is expected when files don't match the target type
                // or when file format is incompatible
                logger.debug("Failed to read file '{}' as {}: {}",
                           file.getName(), clazz.getSimpleName(), e.getMessage());
                return null;
            }
        }
    }
}
