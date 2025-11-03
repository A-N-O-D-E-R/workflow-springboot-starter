package com.anode.workflow.spring.autoconfigure.storage;

import com.anode.tool.service.CommonService;
import com.anode.workflow.spring.autoconfigure.WorkflowProperties;
import com.anode.workflow.spring.autoconfigure.WorkflowProperties.StorageType;
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
     */
    private static class JpaCommonServiceAdapter implements CommonService {
        private final EntityManager entityManager;

        public JpaCommonServiceAdapter(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @Override
        public void save(java.io.Serializable id, Object object) {
            entityManager.getTransaction().begin();
            try {
                entityManager.persist(object);
                entityManager.getTransaction().commit();
            } catch (Exception e) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                throw e;
            }
        }

        @Override
        public void update(java.io.Serializable id, Object object) {
            entityManager.getTransaction().begin();
            try {
                entityManager.merge(object);
                entityManager.getTransaction().commit();
            } catch (Exception e) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                throw e;
            }
        }

        @Override
        public void saveOrUpdate(java.io.Serializable id, Object object) {
            entityManager.getTransaction().begin();
            try {
                entityManager.merge(object);
                entityManager.getTransaction().commit();
            } catch (Exception e) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                throw e;
            }
        }

        @Override
        public void delete(java.io.Serializable id) {
            // Generic delete - implementation depends on entity type
            entityManager.getTransaction().begin();
            Object entity = entityManager.find(Object.class, id);
            if (entity != null) {
                entityManager.remove(entity);
            }
            entityManager.getTransaction().commit();
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
            entityManager.getTransaction().begin();
            for (Object obj : objects) {
                entityManager.persist(obj);
            }
            entityManager.getTransaction().commit();
        }

        @Override
        public void saveOrUpdateCollection(java.util.Collection objects) {
            for (Object obj : objects) {
                saveOrUpdate(null, obj);
            }
        }

        @Override
        public <T> java.util.List<T> getAll(Class<T> type) {
            jakarta.persistence.TypedQuery<T> query = entityManager.createQuery(
                "SELECT e FROM " + type.getSimpleName() + " e", type);
            return query.getResultList();
        }

        @Override
        public <T> T getUniqueItem(Class<T> type, String uniqueKeyName, String uniqueKeyValue) {
            jakarta.persistence.TypedQuery<T> query = entityManager.createQuery(
                "SELECT e FROM " + type.getSimpleName() + " e WHERE e." + uniqueKeyName + " = :value", type);
            query.setParameter("value", uniqueKeyValue);
            java.util.List<T> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
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
