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
        private static final int MAX_ID_LENGTH = 200; // Maximum ID length before .json extension

        private final String basePath;
        private final String canonicalBasePath; // Cached to avoid repeated I/O
        private final ObjectMapper objectMapper;
        private final Map<String, Long> counters = new java.util.concurrent.ConcurrentHashMap<>();
        private final Map<Serializable, java.util.concurrent.locks.ReentrantLock> fileLocks = new java.util.concurrent.ConcurrentHashMap<>();

        public FileCommonService(String basePath) {
            this.basePath = basePath;
            this.objectMapper = new ObjectMapper();

            // Cache canonical base path to avoid repeated I/O operations on every file access
            try {
                this.canonicalBasePath = new File(basePath).getCanonicalPath();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to resolve canonical path for: " + basePath, e);
            }
        }

        /**
         * Get or create a lock for a specific file ID.
         * This enables fine-grained locking per file instead of global synchronization.
         */
        private java.util.concurrent.locks.ReentrantLock getLockForId(Serializable id) {
            return fileLocks.computeIfAbsent(id, k -> new java.util.concurrent.locks.ReentrantLock());
        }

        /**
         * Get the file for the given ID with path traversal protection.
         *
         * <p>Uses cached canonical base path to avoid repeated I/O operations.
         *
         * @param id the entity ID
         * @return the file for the given ID
         * @throws SecurityException if path traversal is detected
         * @throws IllegalStateException if path validation fails
         */
        private File getFile(Serializable id) {
            // Sanitize the ID to prevent path traversal
            String sanitizedId = sanitizeId(id!=null ? id.toString() : null);
            File file = new File(basePath, sanitizedId + ".json");

            // Validate the file is within basePath (prevent path traversal)
            // Use cached canonicalBasePath to avoid repeated File I/O
            try {
                String canonicalFile = file.getCanonicalPath();
                if (!canonicalFile.startsWith(canonicalBasePath + File.separator) &&
                    !canonicalFile.equals(canonicalBasePath)) {
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
         * @throws IllegalArgumentException if ID is null, empty, too long, or becomes empty after sanitization
         */
        private String sanitizeId(String id) {
            // Validate ID is not null or empty
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("ID cannot be null or empty");
            }

            // Validate ID length to prevent filesystem path length issues
            if (id.length() > MAX_ID_LENGTH) {
                throw new IllegalArgumentException(
                    "ID too long: " + id.length() + " chars (max " + MAX_ID_LENGTH + ")");
            }

            // Remove or replace dangerous characters:
            // - Forward and back slashes: / \
            // - Colon (Windows drive): :
            // - Wildcards: * ?
            // - Quotes: " <> (Windows reserved)
            // - Pipe: | (Windows reserved)
            // - Null bytes: \x00
            // - Parent directory references: .. (two or more dots)
            // Replace each dangerous character or pattern with an underscore
            String sanitized = id.replaceAll("[/\\\\:*?\"<>|\\x00]|\\.\\.+", "_");
            // Ensure sanitization didn't result in empty string
            if (sanitized.isEmpty() || sanitized.chars().allMatch(ch -> ch == '_')) {
                throw new IllegalArgumentException(
                    "ID becomes empty after sanitization: " + id);
            }

            return sanitized;
        }

        @Override
        public void save(Serializable id, Object object) {
            // Validate ID early (getFile will also validate, but we need to check before locking)
            if (id == null) {
                throw new IllegalArgumentException("ID cannot be null");
            }
            java.util.concurrent.locks.ReentrantLock lock = getLockForId(id);
            lock.lock();
            try {
                File file = getFile(id);
                if (!file.exists()) {
                    write(file, object);
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void update(Serializable id, Object object) {
            if (id == null) {
                throw new IllegalArgumentException("ID cannot be null");
            }
            java.util.concurrent.locks.ReentrantLock lock = getLockForId(id);
            lock.lock();
            try {
                File file = getFile(id);
                if (file.exists()) {
                    write(file, object);
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void saveOrUpdate(Serializable id, Object object) {
            if (id == null) {
                throw new IllegalArgumentException("ID cannot be null");
            }
            java.util.concurrent.locks.ReentrantLock lock = getLockForId(id);
            lock.lock();
            try {
                write(getFile(id), object);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void delete(Serializable id) {
            if (id == null) {
                throw new IllegalArgumentException("ID cannot be null");
            }
            java.util.concurrent.locks.ReentrantLock lock = getLockForId(id);
            lock.lock();
            try {
                File file = getFile(id);
                if (file.exists()) {
                    if (!file.delete()) {
                        throw new RuntimeException("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            } finally {
                lock.unlock();
                // Clean up the lock to prevent memory leak
                fileLocks.remove(id);
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
        public long incrCounter(String counterName) {
            // Use ConcurrentHashMap's atomic operations for thread-safe counter increment
            return counters.compute(counterName, (k, v) -> v == null ? 1L : v + 1L);
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
