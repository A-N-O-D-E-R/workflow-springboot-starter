package com.anode.workflow.spring.autoconfigure.condition;

import com.anode.workflow.spring.autoconfigure.properties.JpaProperties.StorageType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OnStorageTypeCondition.
 */
class OnStorageTypeConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void shouldMatchWhenStorageTypeIsConfigured() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=test-engine",
                "workflow.engines[0].storage.type=memory"
            )
            .withUserConfiguration(MemoryTestConfig.class)
            .run(context -> {
                assertThat(context).hasBean("memoryBean");
                assertThat(context).getBean("memoryBean").isEqualTo("memory-storage");
            });
    }

    @Test
    void shouldNotMatchWhenStorageTypeIsNotConfigured() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=test-engine",
                "workflow.engines[0].storage.type=file"
            )
            .withUserConfiguration(MemoryTestConfig.class)
            .run(context -> {
                assertThat(context).doesNotHaveBean("memoryBean");
            });
    }

    @Test
    void shouldMatchWhenMultipleEnginesAndOneUsesStorageType() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=engine1",
                "workflow.engines[0].storage.type=file",
                "workflow.engines[1].name=engine2",
                "workflow.engines[1].storage.type=memory"
            )
            .withUserConfiguration(MemoryTestConfig.class)
            .run(context -> {
                assertThat(context).hasBean("memoryBean");
            });
    }

    @Test
    void shouldNotMatchWhenNoEnginesConfigured() {
        contextRunner
            .withUserConfiguration(MemoryTestConfig.class)
            .run(context -> {
                assertThat(context).doesNotHaveBean("memoryBean");
            });
    }

    @Test
    void shouldMatchMultipleConditionsForDifferentStorageTypes() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=engine1",
                "workflow.engines[0].storage.type=memory",
                "workflow.engines[1].name=engine2",
                "workflow.engines[1].storage.type=file"
            )
            .withUserConfiguration(MultiStorageTestConfig.class)
            .run(context -> {
                assertThat(context).hasBean("memoryBean");
                assertThat(context).hasBean("fileBean");
                assertThat(context).doesNotHaveBean("jpaBean");
            });
    }

    @Test
    void shouldMatchJpaStorageType() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=test-engine",
                "workflow.engines[0].storage.type=jpa"
            )
            .withUserConfiguration(JpaTestConfig.class)
            .run(context -> {
                assertThat(context).hasBean("jpaBean");
            });
    }

    @Test
    void shouldMatchFileStorageType() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=test-engine",
                "workflow.engines[0].storage.type=file"
            )
            .withUserConfiguration(FileTestConfig.class)
            .run(context -> {
                assertThat(context).hasBean("fileBean");
            });
    }

    @Configuration
    @ConditionalOnStorageType(StorageType.MEMORY)
    static class MemoryTestConfig {
        @Bean
        public String memoryBean() {
            return "memory-storage";
        }
    }

    @Configuration
    @ConditionalOnStorageType(StorageType.JPA)
    static class JpaTestConfig {
        @Bean
        public String jpaBean() {
            return "jpa-storage";
        }
    }

    @Configuration
    @ConditionalOnStorageType(StorageType.FILE)
    static class FileTestConfig {
        @Bean
        public String fileBean() {
            return "file-storage";
        }
    }

    @Configuration
    static class MultiStorageTestConfig {
        @Bean
        @ConditionalOnStorageType(StorageType.MEMORY)
        public String memoryBean() {
            return "memory-storage";
        }

        @Bean
        @ConditionalOnStorageType(StorageType.JPA)
        public String jpaBean() {
            return "jpa-storage";
        }

        @Bean
        @ConditionalOnStorageType(StorageType.FILE)
        public String fileBean() {
            return "file-storage";
        }
    }
}
