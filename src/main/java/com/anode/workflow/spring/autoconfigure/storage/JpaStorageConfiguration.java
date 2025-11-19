package com.anode.workflow.spring.autoconfigure.storage;

import com.anode.tool.service.CommonService;
import com.anode.workflow.spring.autoconfigure.condition.ConditionalOnStorageType;
import com.anode.workflow.spring.autoconfigure.properties.JpaProperties.StorageType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JPA/Hibernate storage configuration for workflow engine.
 *
 * <p>This bean is only created when at least one engine is configured to use JPA storage:
 * <pre>
 * workflow:
 *   engines:
 *     - name: my-engine
 *       storage:
 *         type: jpa
 * </pre>
 */
@Configuration
@ConditionalOnClass(EntityManager.class)
@ConditionalOnStorageType(StorageType.JPA)
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
     * @return JPA-based common service implementation
     */
    @Bean
    @ConditionalOnMissingBean(name = "jpaCommonService")
    @ConditionalOnBean(EntityManagerFactory.class)
    public CommonService jpaCommonService(EntityManagerFactory entityManagerFactory) {

        logger.info("Configuring JPA-based storage for workflow engine");
        logger.info("JPA storage configured successfully");
        logger.debug("  EntityManagerFactory: {}", entityManagerFactory.getClass().getSimpleName());

        // Return a wrapper that uses entity manager factory (thread-safe)
        return new JpaCommonServiceAdapter(entityManagerFactory);
    }

    /**
     * Adapter class that provides CommonService using EntityManagerFactory.
     *
     * <p>This provides a thread-safe JPA-based storage implementation.
     * <p>Each operation creates its own EntityManager to ensure thread safety.
     * <p>Note: This uses manual transaction management. For production use, consider
     * integrating with Spring's transaction management via @Transactional.
     */
    private static class JpaCommonServiceAdapter implements CommonService {
        private static final Logger logger = LoggerFactory.getLogger(JpaCommonServiceAdapter.class);

        private final EntityManagerFactory entityManagerFactory;

        public JpaCommonServiceAdapter(EntityManagerFactory entityManagerFactory) {
            this.entityManagerFactory = entityManagerFactory;
        }

        /**
         * Get a new EntityManager for the current operation.
         */
        private EntityManager getEntityManager() {
            return entityManagerFactory.createEntityManager();
        }

        /**
         * Execute operation within a transaction with proper rollback handling.
         *
         * <p>This method ensures proper resource cleanup even in case of exceptions:
         * <ul>
         *   <li>EntityManager is always closed in finally block</li>
         *   <li>Transaction is rolled back if active when exception occurs</li>
         *   <li>Rollback exceptions are logged but don't mask the original exception</li>
         * </ul>
         */
        private void executeInTransaction(java.util.function.Consumer<EntityManager> operation) {
            EntityManager em = null;
            jakarta.persistence.EntityTransaction transaction = null;

            try {
                em = getEntityManager();
                transaction = em.getTransaction();
                transaction.begin();
                operation.accept(em);
                transaction.commit();
            } catch (RuntimeException e) {
                // Attempt to rollback if transaction is active
                if (transaction != null && transaction.isActive()) {
                    try {
                        transaction.rollback();
                    } catch (Exception rollbackException) {
                        logger.error("Failed to rollback transaction", rollbackException);
                        // Add rollback exception as suppressed to preserve full error context
                        e.addSuppressed(rollbackException);
                    }
                }
                throw e;
            } finally {
                // Always close EntityManager, even if transaction commit/rollback failed
                if (em != null && em.isOpen()) {
                    try {
                        em.close();
                    } catch (Exception closeException) {
                        // Log but don't throw - we don't want to mask the original exception
                        logger.error("Failed to close EntityManager", closeException);
                    }
                }
            }
        }

        /**
         * Execute query operation with proper resource cleanup.
         *
         * <p>Ensures EntityManager is always closed even if query execution fails.
         */
        private <T> T executeQuery(java.util.function.Function<EntityManager, T> query) {
            EntityManager em = null;
            try {
                em = getEntityManager();
                return query.apply(em);
            } finally {
                // Always close EntityManager, even if query failed
                if (em != null && em.isOpen()) {
                    try {
                        em.close();
                    } catch (Exception closeException) {
                        // Log but don't throw - we don't want to mask the original exception
                        logger.error("Failed to close EntityManager", closeException);
                    }
                }
            }
        }

        @Override
        public void save(java.io.Serializable id, Object object) {
            executeInTransaction(em -> em.persist(object));
        }

        @Override
        public void update(java.io.Serializable id, Object object) {
            executeInTransaction(em -> em.merge(object));
        }

        @Override
        public void saveOrUpdate(java.io.Serializable id, Object object) {
            executeInTransaction(em -> em.merge(object));
        }

        /**
         * Delete operation is not supported in JPA storage.
         *
         * <p><b>Limitation:</b> The {@link CommonService#delete(Serializable)} method only provides
         * an ID without the entity type, making it impossible to perform a JPA delete operation
         * (which requires both the entity class and ID).
         *
         * <p><b>Workaround:</b> Use JPA repositories directly or the {@code get() + EntityManager.remove()}
         * pattern if you know the entity type:
         * <pre>
         * // Instead of: commonService.delete(id);
         * // Use:
         * MyEntity entity = commonService.get(MyEntity.class, id);
         * if (entity != null) {
         *     entityManager.remove(entity);
         * }
         * </pre>
         *
         * @param id the entity ID
         * @throws UnsupportedOperationException always thrown as this operation is not supported
         */
        @Override
        public void delete(java.io.Serializable id) {
            throw new UnsupportedOperationException(
                "Delete by ID alone is not supported in JPA storage. " +
                "The CommonService.delete(Serializable) method doesn't provide the entity type, " +
                "which is required for JPA operations. " +
                "Use type-specific repositories or get() followed by EntityManager.remove() instead.");
        }

        @Override
        public <T> T get(Class<T> objectClass, java.io.Serializable id) {
            return executeQuery(em -> em.find(objectClass, id));
        }

        @Override
        public <T> T getLocked(Class<T> objectClass, java.io.Serializable id) {
            return executeQuery(em ->
                em.find(objectClass, id, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE));
        }

        @Override
        public void saveCollection(java.util.Collection objects) {
            executeInTransaction(em -> {
                for (Object obj : objects) {
                    em.persist(obj);
                }
            });
        }

        @Override
        public void saveOrUpdateCollection(java.util.Collection objects) {
            executeInTransaction(em -> {
                for (Object obj : objects) {
                    em.merge(obj);
                }
            });
        }

        @Override
        public <T> java.util.List<T> getAll(Class<T> type) {
            return executeQuery(em -> {
                // Use entity name from metadata to prevent issues
                String entityName = getEntityName(em, type);
                jakarta.persistence.TypedQuery<T> query = em.createQuery(
                    "SELECT e FROM " + entityName + " e", type);
                return query.getResultList();
            });
        }

        @Override
        public <T> T getUniqueItem(Class<T> type, String uniqueKeyName, String uniqueKeyValue) {
            return executeQuery(em -> {
                // Use JPA Criteria API to avoid SQL injection risks
                // This ensures field names are validated against the metamodel
                try {
                    jakarta.persistence.criteria.CriteriaBuilder cb = em.getCriteriaBuilder();
                    jakarta.persistence.criteria.CriteriaQuery<T> cq = cb.createQuery(type);
                    jakarta.persistence.criteria.Root<T> root = cq.from(type);

                    // This will throw IllegalArgumentException if uniqueKeyName is not a valid attribute
                    cq.select(root).where(cb.equal(root.get(uniqueKeyName), uniqueKeyValue));

                    java.util.List<T> results = em.createQuery(cq).getResultList();
                    return results.isEmpty() ? null : results.get(0);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        "Invalid field name '" + uniqueKeyName + "' for entity type " + type.getName() +
                        ". The field does not exist in the entity metamodel.", e);
                }
            });
        }

        /**
         * Get the entity name for JPQL queries using JPA metadata.
         *
         * @param em the entity manager
         * @param type the entity class
         * @return the entity name
         */
        private <T> String getEntityName(EntityManager em, Class<T> type) {
            try {
                jakarta.persistence.metamodel.EntityType<T> entityType =
                    em.getMetamodel().entity(type);
                return entityType.getName();
            } catch (IllegalArgumentException e) {
                // Fallback to simple name if not a managed entity
                return type.getSimpleName();
            }
        }

        @Override
        public long incrCounter(String counterName) {
            throw new UnsupportedOperationException(
                "incrCounter is not supported in JPA storage. " +
                "The CommonService interface doesn't provide entity type information needed for JPA. " +
                "Implement a dedicated counter table or use application-level counter management instead.");
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
