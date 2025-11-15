package com.anode.workflow.spring.autoconfigure.storage;

import com.anode.tool.service.CommonService;
import com.anode.workflow.spring.autoconfigure.condition.ConditionalOnStorageType;
import com.anode.workflow.spring.autoconfigure.properties.JpaProperties.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory storage configuration for workflow engine.
 *
 * <p>This bean is only created when at least one engine is configured to use memory storage:
 * <pre>
 * workflow:
 *   engines:
 *     - name: my-engine
 *       storage:
 *         type: memory
 * </pre>
 *
 * <p><b>WARNING:</b> This is suitable for testing and development only.
 * All data is lost when the application restarts.
 */
@Configuration
@ConditionalOnStorageType(StorageType.MEMORY)
public class MemoryStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MemoryStorageConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(name = "memoryCommonService")
    public CommonService memoryCommonService() {
        logger.warn("Configuring IN-MEMORY storage for workflow engine");
        logger.warn("  ⚠️  All workflow data will be lost on application restart");
        logger.warn("  ⚠️  Use only for development and testing");

        return new MemoryCommonService();
    }

    /**
     * Simple in-memory implementation of CommonService.
     *
     * <p>Uses HashMaps to store all entities in memory.
     */
    private static class MemoryCommonService implements CommonService {
        private final Map<Serializable, Object> storage = new HashMap<>();
        private final Map<String, Long> counters = new HashMap<>();
        private final Map<Serializable, Boolean> locks = new HashMap<>();

        @Override
        public synchronized void save(Serializable id, Object object) {
            storage.putIfAbsent(id, object);
        }

        @Override
        public synchronized void update(Serializable id, Object object) {
            if (storage.containsKey(id)) {
                storage.put(id, object);
            }
        }

        @Override
        public synchronized void saveOrUpdate(Serializable id, Object object) {
            storage.put(id, object);
        }

        @Override
        public synchronized void delete(Serializable id) {
            storage.remove(id);
            locks.remove(id);
        }

        @Override
        public synchronized <T> T get(Class<T> objectClass, Serializable id) {
            Object obj = storage.get(id);
            return objectClass.isInstance(obj) ? objectClass.cast(obj) : null;
        }

        @Override
        public synchronized <T> T getLocked(Class<T> objectClass, Serializable id) {
            if (!storage.containsKey(id)) {
                throw new IllegalArgumentException("Object with ID " + id + " does not exist");
            }

            if (locks.getOrDefault(id, false)) {
                throw new IllegalStateException("Object with ID " + id + " is already locked");
            }

            locks.put(id, true);
            return get(objectClass, id);
        }


        @Override
        public synchronized void saveCollection(Collection objects) {
            for (Object obj : objects) {
                // Generate unique ID for each object
                Serializable id = java.util.UUID.randomUUID().toString();
                save(id, obj);
            }
        }

        @Override
        public synchronized void saveOrUpdateCollection(Collection objects) {
            for (Object obj : objects) {
                // Generate unique ID for each object
                Serializable id = java.util.UUID.randomUUID().toString();
                saveOrUpdate(id, obj);
            }
        }

        @Override
        public <T> List<T> getAll(Class<T> type) {
            return storage.values().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
        }

        @Override
        public <T> T getUniqueItem(Class<T> type, String uniqueKeyName, String uniqueKeyValue) {
            return storage.values().stream()
                .filter(type::isInstance)
                .map(type::cast)
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
            throw new UnsupportedOperationException("makeClone not implemented in memory storage");
        }

        @Override
        public Serializable getMinimalId(Comparator<Serializable> comparator) {
            return storage.keySet().stream()
                .min(comparator)
                .orElse(null);
        }
    }
}
