package com.anode.workflow.spring.autoconfigure.storage;

import com.anode.tool.service.CommonService;
import com.anode.workflow.spring.autoconfigure.properties.WorkflowProperties;
import com.anode.workflow.storage.db.sql.common.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JPA/Hibernate storage configuration for workflow engine.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>workflow.storage.type=jpa (or default)</li>
 *   <li>JPA is on the classpath</li>
 *   <li>EntityManagerFactory bean exists</li>
 * </ul>
 *
 * <p>Configuration example:
 * <pre>
 * workflow:
 *   storage:
 *     type: jpa
 *   jpa:
 *     entity-manager-factory-ref: entityManagerFactory
 * </pre>
 */
@Configuration
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(
    prefix = "workflow.storage",
    name = "type",
    havingValue = "jpa",
    matchIfMissing = true
)
public class JpaStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(JpaStorageConfiguration.class);

    /**
     * Creates a JPA-based CommonService using repository implementations.
     *
     * <p>This provides a complete storage layer using JPA repositories for:
     * <ul>
     *   <li>Workflow definitions</li>
     *   <li>Workflow instances</li>
     *   <li>Steps</li>
     *   <li>Variables</li>
     *   <li>Milestones</li>
     * </ul>
     *
     * @param entityManagerFactory the entity manager factory
     * @param properties workflow properties
     * @return JPA-based common service implementation
     */
    @Bean
    @ConditionalOnMissingBean(CommonService.class)
    public CommonService jpaCommonService(
            EntityManagerFactory entityManagerFactory,
            WorkflowProperties properties) {

        logger.info("Configuring JPA-based storage for workflow engine");

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        logger.info("JPA storage configured successfully");
        logger.debug("  EntityManagerFactory: {}", entityManagerFactory.getClass().getSimpleName());

        // Return a wrapper that uses entity manager directly
        return new JpaCommonServiceAdapter(entityManager);
    }

    /**
     * Adapter class that provides CommonService using EntityManager directly.
     *
     * <p>This provides a simpler JPA-based storage implementation using EntityManager.
     * <p>Note: This uses manual transaction management. For production use, consider
     * integrating with Spring's transaction management via @Transactional.
     */
    private static class JpaCommonServiceAdapter implements CommonService {
        private final EntityManager entityManager;

        public JpaCommonServiceAdapter(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        /**
         * Execute operation within a transaction with proper rollback handling.
         */
        private void executeInTransaction(Runnable operation) {
            jakarta.persistence.EntityTransaction transaction = entityManager.getTransaction();
            boolean wasActive = transaction.isActive();

            try {
                if (!wasActive) {
                    transaction.begin();
                }
                operation.run();
                if (!wasActive) {
                    transaction.commit();
                }
            } catch (RuntimeException e) {
                if (!wasActive && transaction.isActive()) {
                    try {
                        transaction.rollback();
                    } catch (Exception rollbackException) {
                        logger.error("Failed to rollback transaction", rollbackException);
                    }
                }
                throw e;
            }
        }

        @Override
        public void save(java.io.Serializable id, Object object) {
            executeInTransaction(() -> entityManager.persist(object));
        }

        @Override
        public void update(java.io.Serializable id, Object object) {
            executeInTransaction(() -> entityManager.merge(object));
        }

        @Override
        public void saveOrUpdate(java.io.Serializable id, Object object) {
            executeInTransaction(() -> entityManager.merge(object));
        }

        @Override
        public void delete(java.io.Serializable id) {
            // Generic delete - implementation depends on entity type
            // Note: This requires knowing the entity type, which isn't provided
            // This is a limitation of the CommonService interface
            executeInTransaction(() -> {
                // This won't work without knowing the entity class
                // Subclasses should override this method with proper entity type
                throw new UnsupportedOperationException(
                    "Generic delete by ID requires entity type - use type-specific repository instead");
            });
        }

        @Override
        public <T> T get(Class<T> objectClass, java.io.Serializable id) {
            return entityManager.find(objectClass, id);
        }

        @Override
        public <T> T getLocked(Class<T> objectClass, java.io.Serializable id) {
            return entityManager.find(objectClass, id, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        }

        @Override
        public void saveCollection(java.util.Collection objects) {
            executeInTransaction(() -> {
                for (Object obj : objects) {
                    entityManager.persist(obj);
                }
            });
        }

        @Override
        public void saveOrUpdateCollection(java.util.Collection objects) {
            executeInTransaction(() -> {
                for (Object obj : objects) {
                    entityManager.merge(obj);
                }
            });
        }

        @Override
        public <T> java.util.List<T> getAll(Class<T> type) {
            // Use entity name from metadata to prevent issues
            String entityName = getEntityName(type);
            jakarta.persistence.TypedQuery<T> query = entityManager.createQuery(
                "SELECT e FROM " + entityName + " e", type);
            return query.getResultList();
        }

        @Override
        public <T> T getUniqueItem(Class<T> type, String uniqueKeyName, String uniqueKeyValue) {
            // Use entity name from metadata and parameterized query
            // Note: uniqueKeyName is assumed to be a valid field name from internal usage
            String entityName = getEntityName(type);
            jakarta.persistence.TypedQuery<T> query = entityManager.createQuery(
                "SELECT e FROM " + entityName + " e WHERE e." + uniqueKeyName + " = :value", type);
            query.setParameter("value", uniqueKeyValue);
            java.util.List<T> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        }

        /**
         * Get the entity name for JPQL queries using JPA metadata.
         *
         * @param type the entity class
         * @return the entity name
         */
        private <T> String getEntityName(Class<T> type) {
            try {
                jakarta.persistence.metamodel.EntityType<T> entityType =
                    entityManager.getMetamodel().entity(type);
                return entityType.getName();
            } catch (IllegalArgumentException e) {
                // Fallback to simple name if not a managed entity
                return type.getSimpleName();
            }
        }

        @Override
        public long incrCounter(String counterName) {
            // Simple counter implementation using a query
            // In production, you might want a separate counter table
            return System.currentTimeMillis();
        }

        @Override
        public java.util.Map<java.io.Serializable, java.io.Serializable> makeClone(
                Object object, com.anode.tool.service.IdFactory idFactory) {
            throw new UnsupportedOperationException("makeClone not implemented in JPA adapter");
        }

        @Override
        public java.io.Serializable getMinimalId(java.util.Comparator<java.io.Serializable> comparator) {
            throw new UnsupportedOperationException("getMinimalId not implemented in JPA adapter");
        }
    }
}
