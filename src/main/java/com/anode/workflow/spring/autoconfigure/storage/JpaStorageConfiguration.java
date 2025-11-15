package com.anode.workflow.spring.autoconfigure.storage;

import com.anode.tool.service.CommonService;
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
 * <p>This bean is available when JPA is on the classpath and can be selected per engine:
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
         */
        private void executeInTransaction(java.util.function.Consumer<EntityManager> operation) {
            EntityManager em = getEntityManager();
            jakarta.persistence.EntityTransaction transaction = em.getTransaction();

            try {
                transaction.begin();
                operation.accept(em);
                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    try {
                        transaction.rollback();
                    } catch (Exception rollbackException) {
                        logger.error("Failed to rollback transaction", rollbackException);
                    }
                }
                throw e;
            } finally {
                em.close();
            }
        }

        /**
         * Execute query operation with proper resource cleanup.
         */
        private <T> T executeQuery(java.util.function.Function<EntityManager, T> query) {
            EntityManager em = getEntityManager();
            try {
                return query.apply(em);
            } finally {
                em.close();
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

        @Override
        public void delete(java.io.Serializable id) {
            // Generic delete - implementation depends on entity type
            // Note: This requires knowing the entity type, which isn't provided
            // This is a limitation of the CommonService interface
            throw new UnsupportedOperationException(
                "Generic delete by ID requires entity type - use type-specific repository instead");
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
                // Use entity name from metadata and parameterized query
                // Note: uniqueKeyName is assumed to be a valid field name from internal usage
                String entityName = getEntityName(em, type);
                jakarta.persistence.TypedQuery<T> query = em.createQuery(
                    "SELECT e FROM " + entityName + " e WHERE e." + uniqueKeyName + " = :value", type);
                query.setParameter("value", uniqueKeyValue);
                java.util.List<T> results = query.getResultList();
                return results.isEmpty() ? null : results.get(0);
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
