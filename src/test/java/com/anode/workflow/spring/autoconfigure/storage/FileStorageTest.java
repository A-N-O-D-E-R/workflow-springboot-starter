package com.anode.workflow.spring.autoconfigure.storage;

import com.anode.tool.service.CommonService;
import com.anode.workflow.spring.autoconfigure.properties.JpaProperties.StorageType;
import com.anode.workflow.spring.autoconfigure.properties.WorkflowEnginesProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link FileStorageConfiguration} including security features.
 */
class FileStorageTest {

    @TempDir
    Path tempDir;

    private CommonService fileStorage;
    private String basePath;

    @BeforeEach
    void setUp() {
        basePath = tempDir.toString();

        // Create properties with file storage configuration
        WorkflowEnginesProperties properties = new WorkflowEnginesProperties();
        WorkflowEnginesProperties.EngineConfig engineConfig = new WorkflowEnginesProperties.EngineConfig();
        engineConfig.setName("test-engine");

        com.anode.workflow.spring.autoconfigure.properties.StorageProperties storageProps =
            new com.anode.workflow.spring.autoconfigure.properties.StorageProperties();
        storageProps.setType(StorageType.FILE);
        storageProps.setFilePath(basePath);

        engineConfig.setStorage(storageProps);
        properties.setEngines(List.of(engineConfig));

        FileStorageConfiguration config = new FileStorageConfiguration();
        fileStorage = config.fileCommonService(properties);
    }

    @AfterEach
    void tearDown() {
        // Clean up test files
        File dir = new File(basePath);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    @Test
    void shouldSaveAndRetrieveObject() {
        // Given
        TestEntity entity = new TestEntity("test-id", "test-value");

        // When
        fileStorage.save("test-id", entity);
        TestEntity retrieved = fileStorage.get(TestEntity.class, "test-id");

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo("test-id");
        assertThat(retrieved.value()).isEqualTo("test-value");
    }

    @Test
    void shouldPreventPathTraversalWithDotDot() {
        // Given - attempt to escape directory with ../
        TestEntity entity = new TestEntity("test", "value");
        Serializable maliciousId = "../../../etc/passwd";

        // When - should sanitize the ID
        fileStorage.save(maliciousId, entity);

        // Then - file should be created in base directory with sanitized name
        // ../../../etc/passwd becomes ______etc_passwd
        // (.. → _, / → _, .. → _, / → _, .. → _, / → _, etc/passwd → etc_passwd)
        File expectedFile = new File(basePath, "______etc_passwd.json");
        assertThat(expectedFile).exists();

        // And should be retrievable with the same ID
        TestEntity retrieved = fileStorage.get(TestEntity.class, maliciousId);
        assertThat(retrieved).isNotNull();
    }

    @Test
    void shouldPreventPathTraversalWithForwardSlash() {
        // Given - attempt to create subdirectory
        TestEntity entity = new TestEntity("test", "value");
        Serializable maliciousId = "subdir/file";

        // When
        fileStorage.save(maliciousId, entity);

        // Then - slashes should be replaced
        File expectedFile = new File(basePath, "subdir_file.json");
        assertThat(expectedFile).exists();

        // Subdirectory should NOT be created
        File subdir = new File(basePath, "subdir");
        assertThat(subdir).doesNotExist();
    }

    @Test
    void shouldPreventPathTraversalWithBackslash() {
        // Given - Windows-style path traversal
        TestEntity entity = new TestEntity("test", "value");
        Serializable maliciousId = "subdir\\file";

        // When
        fileStorage.save(maliciousId, entity);

        // Then - backslashes should be replaced
        File expectedFile = new File(basePath, "subdir_file.json");
        assertThat(expectedFile).exists();
    }

    @Test
    void shouldPreventNullByteInjection() {
        // Given - null byte attack
        TestEntity entity = new TestEntity("test", "value");
        Serializable maliciousId = "file\u0000.txt";

        // When
        fileStorage.save(maliciousId, entity);

        // Then - null bytes should be replaced
        File expectedFile = new File(basePath, "file_.txt.json");
        assertThat(expectedFile).exists();
    }

    @Test
    void shouldHandleMultipleDotDotSequences() {
        // Given - multiple .. sequences
        TestEntity entity = new TestEntity("test", "value");
        Serializable maliciousId = "../../..";

        // When
        fileStorage.save(maliciousId, entity);

        // Then - all dangerous patterns should be replaced
        // ../../.. becomes _____
        // (.. → _, / → _, .. → _, / → _, .. → _)
        File expectedFile = new File(basePath, "_____.json");
        assertThat(expectedFile).exists();
    }

    @Test
    void shouldDeleteFile() {
        // Given
        fileStorage.save("test-id", new TestEntity("test-id", "value"));

        // When
        fileStorage.delete("test-id");

        // Then
        TestEntity retrieved = fileStorage.get(TestEntity.class, "test-id");
        assertThat(retrieved).isNull();
    }

    @Test
    void shouldUpdateExistingFile() {
        // Given
        fileStorage.save("test-id", new TestEntity("test-id", "original"));

        // When
        fileStorage.update("test-id", new TestEntity("test-id", "updated"));

        // Then
        TestEntity retrieved = fileStorage.get(TestEntity.class, "test-id");
        assertThat(retrieved.value()).isEqualTo("updated");
    }

    @Test
    void shouldReturnNullForNonExistentFile() {
        // When
        TestEntity retrieved = fileStorage.get(TestEntity.class, "non-existent");

        // Then
        assertThat(retrieved).isNull();
    }

    @Test
    void shouldIncrementCounter() {
        // When
        long value1 = fileStorage.incrCounter("test-counter");
        long value2 = fileStorage.incrCounter("test-counter");
        long value3 = fileStorage.incrCounter("test-counter");

        // Then
        assertThat(value1).isEqualTo(1);
        assertThat(value2).isEqualTo(2);
        assertThat(value3).isEqualTo(3);
    }

    @Test
    void shouldGetAllObjectsOfType() {
        // Given
        fileStorage.save("id1", new TestEntity("id1", "value1"));
        fileStorage.save("id2", new TestEntity("id2", "value2"));
        fileStorage.save("id3", new OtherEntity("other"));

        // When
        List<TestEntity> entities = fileStorage.getAll(TestEntity.class);

        // Then
        assertThat(entities).hasSize(2);
        assertThat(entities).extracting(TestEntity::id).containsExactlyInAnyOrder("id1", "id2");
    }

    @Test
    void shouldGetUniqueItemByField() {
        // Given
        TestEntity entity1 = new TestEntity("id1", "unique-value");
        TestEntity entity2 = new TestEntity("id2", "other-value");
        fileStorage.save("id1", entity1);
        fileStorage.save("id2", entity2);

        // When
        TestEntity found = fileStorage.getUniqueItem(TestEntity.class, "value", "unique-value");

        // Then
        assertThat(found).isNotNull();
        assertThat(found.id()).isEqualTo("id1");
    }

    @Test
    void shouldHandleNormalIdsCorrectly() {
        // Given - normal alphanumeric IDs
        String[] normalIds = {"test123", "user-456", "workflow_789", "CamelCaseId"};

        for (String id : normalIds) {
            // When
            fileStorage.save(id, new TestEntity(id, "value"));

            // Then
            TestEntity retrieved = fileStorage.get(TestEntity.class, id);
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.id()).isEqualTo(id);
        }
    }

    // Test entities
    record TestEntity(String id, String value) implements Serializable {}
    record OtherEntity(String data) implements Serializable {}
}
