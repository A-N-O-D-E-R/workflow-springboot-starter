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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
     * <p>Uses concurrent data structures to store all entities in memory safely.
     * <p>Provides thread-safe locking mechanism with explicit lock release.
     */
    private static class MemoryCommonService implements CommonService {
        private final Map<Serializable, Object> storage = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        private final Map<Serializable, AtomicBoolean> locks = new ConcurrentHashMap<>();

        @Override
        public void save(Serializable id, Object object) {
            storage.putIfAbsent(id, object);
        }

        @Override
        public void update(Serializable id, Object object) {
            storage.computeIfPresent(id, (k, v) -> object);
        }

        @Override
        public void saveOrUpdate(Serializable id, Object object) {
            storage.put(id, object);
        }

        @Override
        public void delete(Serializable id) {
            storage.remove(id);
            locks.remove(id);
        }

        @Override
        public <T> T get(Class<T> objectClass, Serializable id) {
            Object obj = storage.get(id);
            return objectClass.isInstance(obj) ? objectClass.cast(obj) : null;
        }

        @Override
        public <T> T getLocked(Class<T> objectClass, Serializable id) {
            if (!storage.containsKey(id)) {
                throw new IllegalArgumentException("Object with ID " + id + " does not exist");
            }

            // Use atomic compareAndSet to ensure thread-safe locking
            AtomicBoolean lock = locks.computeIfAbsent(id, k -> new AtomicBoolean(false));

            if (!lock.compareAndSet(false, true)) {
                throw new IllegalStateException("Object with ID " + id + " is already locked");
            }

            return get(objectClass, id);
        }

        /**
         * Releases the lock on an object.
         *
         * <p>This method should be called after processing a locked object to prevent deadlocks.
         *
         * @param id the object ID to unlock
         * @throws IllegalStateException if the object is not locked
         */
        public void unlock(Serializable id) {
            AtomicBoolean lock = locks.get(id);

            if (lock == null || !lock.get()) {
                throw new IllegalStateException("Object with ID " + id + " is not locked");
            }

            if (!lock.compareAndSet(true, false)) {
                throw new IllegalStateException("Failed to release lock for object with ID " + id);
            }
        }

        /**
         * Checks if an object is currently locked.
         *
         * @param id the object ID
         * @return true if the object is locked, false otherwise
         */
        public boolean isLocked(Serializable id) {
            AtomicBoolean lock = locks.get(id);
            return lock != null && lock.get();
        }


        @Override
        public void saveCollection(Collection objects) {
            for (Object obj : objects) {
                // Generate unique ID for each object
                Serializable id = java.util.UUID.randomUUID().toString();
                save(id, obj);
            }
        }

        @Override
        public void saveOrUpdateCollection(Collection objects) {
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
                // Field doesn't exist or isn't accessible - this is expected for mismatched types
                return false;
            }
        }

        @Override
        public long incrCounter(String counterName) {
            AtomicLong counter = counters.computeIfAbsent(counterName, k -> new AtomicLong(0));
            return counter.incrementAndGet();
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
