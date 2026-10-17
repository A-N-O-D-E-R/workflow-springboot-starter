package com.anode.workflow.spring.autoconfigure.storage;

import com.anode.tool.service.CommonService;
import com.anode.workflow.spring.autoconfigure.properties.StorageProperties.StorageType;
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

import com.anode.workflow.entities.workflows.WorkflowDefinition;

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
        String entity = text("test-id", "test-value");

        // When
        fileStorage.save("test-id", entity);
        String retrieved = fileStorage.get(String.class, "test-id");

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(id(retrieved)).isEqualTo("test-id");
        assertThat(value(retrieved)).isEqualTo("test-value");
    }

    @Test
    void shouldPreventPathTraversalWithDotDot() {
        // Given - attempt to escape directory with ../
        String entity = text("test", "value");
        Serializable maliciousId = "../../../etc/passwd";

        // When - should sanitize the ID
        fileStorage.save(maliciousId, entity);

        // Then - file should be created in base directory with sanitized name
        // ../../../etc/passwd becomes ______etc_passwd
        // (.. → _, / → _, .. → _, / → _, .. → _, / → _, etc/passwd → etc_passwd)
        File expectedFile = new File(basePath, "______etc_passwd.json");
        assertThat(expectedFile).exists();

        // And should be retrievable with the same ID
        String retrieved = fileStorage.get(String.class, maliciousId);
        assertThat(retrieved).isNotNull();
    }

    @Test
    void shouldPreventPathTraversalWithForwardSlash() {
        // Given - attempt to create subdirectory
        String entity = text("test", "value");
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
        String entity = text("test", "value");
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
        String entity = text("test", "value");
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
        String entity = text("test", "value");
        Serializable maliciousId = "../../../elements";

        // When
        fileStorage.save(maliciousId, entity);

        // Then - all dangerous patterns should be replaced
        // ../../.. becomes _____
        // (.. → _, / → _, .. → _, / → _, .. → _)
        File expectedFile = new File(basePath, "______elements.json");
        assertThat(expectedFile).exists();
    }

    @Test
    void shouldDeleteFile() {
        // Given
        fileStorage.save("test-id", text("test-id", "value"));

        // When
        fileStorage.delete("test-id");

        // Then
        String retrieved = fileStorage.get(String.class, "test-id");
        assertThat(retrieved).isNull();
    }

    @Test
    void shouldUpdateExistingFile() {
        // Given
        fileStorage.save("test-id", text("test-id", "original"));

        // When
        fileStorage.update("test-id", text("test-id", "updated"));

        // Then
        String retrieved = fileStorage.get(String.class, "test-id");
        assertThat(value(retrieved)).isEqualTo("updated");
    }

    @Test
    void shouldReturnNullForNonExistentFile() {
        // When
        String retrieved = fileStorage.get(String.class, "non-existent");

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
        fileStorage.save("id1", text("id1", "value1"));
        fileStorage.save("id2", text("id2", "value2"));
        fileStorage.save("id3", definition("other-kind"));

        // When
        List<String> entities = fileStorage.getAll(String.class);

        // Then
        assertThat(entities).hasSize(2);
        assertThat(entities).extracting(FileStorageTest::id).containsExactlyInAnyOrder("id1", "id2");
    }

    @Test
    void shouldGetUniqueItemByField() {
        fileStorage.save("journey_1", definition("order"));
        fileStorage.save("journey_2", definition("refund"));

        WorkflowDefinition found = fileStorage.getUniqueItem(WorkflowDefinition.class, "name", "refund");

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("refund");
    }

    @Test
    void shouldHandleNormalIdsCorrectly() {
        // Given - normal alphanumeric IDs
        String[] normalIds = {"test123", "user-456", "workflow_789", "CamelCaseId"};

        for (String id : normalIds) {
            // When
            fileStorage.save(id, text(id, "value"));

            // Then
            String retrieved = fileStorage.get(String.class, id);
            assertThat(retrieved).isNotNull();
            assertThat(id(retrieved)).isEqualTo(id);
        }
    }

    @Test
    void shouldRejectEmptyId() {
        // Given
        String entity = text("", "value");

        // When/Then
        assertThatThrownBy(() -> fileStorage.save("", entity))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ID cannot be null or empty");
    }

    @Test
    void shouldRejectNullId() {
        // Given
        String entity = text("test", "value");

        // When/Then - More precise error message after concurrency fix
        assertThatThrownBy(() -> fileStorage.save(null, entity))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ID cannot be null");
    }

    @Test
    void shouldRejectVeryLongId() {
        // Given - ID longer than 200 characters
        String veryLongId = "a".repeat(201);
        String entity = text("test", "value");

        // When/Then
        assertThatThrownBy(() -> fileStorage.save(veryLongId, entity))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ID too long");
    }

    @Test
    void shouldAcceptMaxLengthId() {
        // Given - ID exactly 200 characters (max allowed)
        String maxLengthId = "a".repeat(200);
        String entity = text("test", "value");

        // When
        fileStorage.save(maxLengthId, entity);

        // Then - should succeed
        String retrieved = fileStorage.get(String.class, maxLengthId);
        assertThat(retrieved).isNotNull();
    }

    @Test
    void shouldRejectIdThatBecomesEmptyAfterSanitization() {
        // Given - ID that contains only dangerous characters
        String allDangerousId = "///...\\\\\\";
        String entity = text("test", "value");

        // When/Then
        assertThatThrownBy(() -> fileStorage.save(allDangerousId, entity))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ID becomes empty after sanitization");
    }

    // Test entities
    /** The file storage keeps engine objects; audit-log text is the simplest one. */
    private static String text(String id, String value) {
        return id + "=" + value;
    }

    private static String id(String text) {
        return text.substring(0, text.indexOf('='));
    }

    private static String value(String text) {
        return text.substring(text.indexOf('=') + 1);
    }

    private static WorkflowDefinition definition(String name) {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setName(name);
        return definition;
    }
}
