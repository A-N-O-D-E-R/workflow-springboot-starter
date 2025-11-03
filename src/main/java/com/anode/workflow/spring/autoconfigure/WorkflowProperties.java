package com.anode.workflow.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Workflow Engine.
 *
 * <p>These properties can be configured in application.properties or application.yml:
 *
 * <pre>
 * workflow:
 *   enabled: true
 *   storage:
 *     type: jpa  # or memory, file, custom
 *     file-path: ./workflow-data  # for file storage
 *   jpa:
 *     enabled: true
 *     entity-manager-factory-ref: entityManagerFactory
 *   event-handler:
 *     enabled: true
 *     bean-name: customEventHandler  # optional custom bean
 *   sla:
 *     enabled: true
 *     queue-manager-bean: customSlaQueueManager  # optional
 * </pre>
 */
@ConfigurationProperties(prefix = "workflow")
public class WorkflowProperties {

    /**
     * Enable or disable workflow engine auto-configuration.
     */
    private boolean enabled = true;

    /**
     * Storage configuration.
     */
    private Storage storage = new Storage();

    /**
     * JPA configuration.
     */
    private Jpa jpa = new Jpa();

    /**
     * Event handler configuration.
     */
    private EventHandler eventHandler = new EventHandler();

    /**
     * SLA (Service Level Agreement) configuration.
     */
    private Sla sla = new Sla();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Jpa getJpa() {
        return jpa;
    }

    public void setJpa(Jpa jpa) {
        this.jpa = jpa;
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public Sla getSla() {
        return sla;
    }

    public void setSla(Sla sla) {
        this.sla = sla;
    }

    /**
     * Storage configuration.
     */
    public static class Storage {
        /**
         * Storage type: jpa, memory, file, or custom.
         */
        private StorageType type = StorageType.JPA;

        /**
         * File path for file-based storage.
         */
        private String filePath = "./workflow-data";

        /**
         * Custom storage bean name (if type is CUSTOM).
         */
        private String customBeanName;

        public StorageType getType() {
            return type;
        }

        public void setType(StorageType type) {
            this.type = type;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getCustomBeanName() {
            return customBeanName;
        }

        public void setCustomBeanName(String customBeanName) {
            this.customBeanName = customBeanName;
        }
    }

    /**
     * JPA configuration.
     */
    public static class Jpa {
        /**
         * Enable JPA-based storage.
         */
        private boolean enabled = true;

        /**
         * EntityManagerFactory bean name.
         */
        private String entityManagerFactoryRef = "entityManagerFactory";

        /**
         * Enable automatic schema creation.
         */
        private boolean autoCreateSchema = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEntityManagerFactoryRef() {
            return entityManagerFactoryRef;
        }

        public void setEntityManagerFactoryRef(String entityManagerFactoryRef) {
            this.entityManagerFactoryRef = entityManagerFactoryRef;
        }

        public boolean isAutoCreateSchema() {
            return autoCreateSchema;
        }

        public void setAutoCreateSchema(boolean autoCreateSchema) {
            this.autoCreateSchema = autoCreateSchema;
        }
    }

    /**
     * Event handler configuration.
     */
    public static class EventHandler {
        /**
         * Enable default event handler.
         */
        private boolean enabled = true;

        /**
         * Custom event handler bean name.
         */
        private String beanName;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBeanName() {
            return beanName;
        }

        public void setBeanName(String beanName) {
            this.beanName = beanName;
        }
    }

    /**
     * SLA configuration.
     */
    public static class Sla {
        /**
         * Enable SLA queue manager.
         */
        private boolean enabled = false;

        /**
         * Custom SLA queue manager bean name.
         */
        private String queueManagerBean;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getQueueManagerBean() {
            return queueManagerBean;
        }

        public void setQueueManagerBean(String queueManagerBean) {
            this.queueManagerBean = queueManagerBean;
        }
    }

    /**
     * Storage type enumeration.
     */
    public enum StorageType {
        /**
         * JPA/Hibernate-based storage (recommended for production).
         */
        JPA,

        /**
         * In-memory storage (for testing).
         */
        MEMORY,

        /**
         * File-based JSON storage (for development/testing).
         */
        FILE,

        /**
         * Custom storage implementation.
         */
        CUSTOM
    }
}
