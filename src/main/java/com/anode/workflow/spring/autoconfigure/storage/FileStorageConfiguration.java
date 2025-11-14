package com.anode.workflow.spring.autoconfigure.storage;

import com.anode.tool.service.CommonService;
import com.anode.workflow.spring.autoconfigure.properties.WorkflowProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * File-based JSON storage configuration for workflow engine.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>workflow.storage.type=file</li>
 * </ul>
 *
 * <p>Configuration example:
 * <pre>
 * workflow:
 *   storage:
 *     type: file
 *     file-path: ./workflow-data
 * </pre>
 *
 * <p><b>WARNING:</b> This is suitable for development and small-scale testing only.
 * For production use, prefer JPA storage.
 */
@Configuration
@ConditionalOnProperty(prefix = "workflow.storage", name = "type", havingValue = "file")
public class FileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(CommonService.class)
    public CommonService fileCommonService(WorkflowProperties properties) {
        String filePath = properties.getStorage().getFilePath();

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

        private File getFile(Serializable id) {
            return new File(basePath, id + ".json");
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
                file.delete();
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
                try {
                    T obj = read(file, type);
                    if (obj != null) {
                        results.add(obj);
                    }
                } catch (Exception e) {
                    // Skip files that don't match the type
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

        private boolean matchesUniqueKey(Object obj, String fieldName, String value) {
            try {
                java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object fieldValue = field.get(obj);
                return value.equals(fieldValue);
            } catch (Exception e) {
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
                return null;
            }
        }
    }
}
