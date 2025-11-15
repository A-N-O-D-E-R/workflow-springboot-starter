package com.anode.workflow.spring.autoconfigure.storage;

import com.anode.tool.service.CommonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.Serializable;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for in-memory storage implementation.
 */
class MemoryStorageTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();
    private CommonService storage;

    @BeforeEach
    void setUp() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=test-engine",
                "workflow.engines[0].storage.type=memory"
            )
            .withUserConfiguration(MemoryStorageConfiguration.class)
            .run(context -> {
                storage = context.getBean("memoryCommonService", CommonService.class);
            });
    }

    @Test
    void shouldSaveAndGetObject() {
        TestEntity entity = new TestEntity("1", "test-name");

        storage.save("1", entity);
        TestEntity retrieved = storage.get(TestEntity.class, "1");

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo("1");
        assertThat(retrieved.getName()).isEqualTo("test-name");
    }

    @Test
    void shouldNotSaveIfAlreadyExists() {
        TestEntity entity1 = new TestEntity("1", "name1");
        TestEntity entity2 = new TestEntity("1", "name2");

        storage.save("1", entity1);
        storage.save("1", entity2); // Should not overwrite

        TestEntity retrieved = storage.get(TestEntity.class, "1");
        assertThat(retrieved.getName()).isEqualTo("name1");
    }

    @Test
    void shouldUpdateExistingObject() {
        TestEntity entity = new TestEntity("1", "original");
        storage.save("1", entity);

        TestEntity updated = new TestEntity("1", "updated");
        storage.update("1", updated);

        TestEntity retrieved = storage.get(TestEntity.class, "1");
        assertThat(retrieved.getName()).isEqualTo("updated");
    }

    @Test
    void shouldNotUpdateNonExistentObject() {
        TestEntity entity = new TestEntity("1", "test");

        storage.update("999", entity);

        TestEntity retrieved = storage.get(TestEntity.class, "999");
        assertThat(retrieved).isNull();
    }

    @Test
    void shouldSaveOrUpdateNewObject() {
        TestEntity entity = new TestEntity("1", "test");

        storage.saveOrUpdate("1", entity);

        TestEntity retrieved = storage.get(TestEntity.class, "1");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("test");
    }

    @Test
    void shouldSaveOrUpdateExistingObject() {
        TestEntity entity1 = new TestEntity("1", "original");
        storage.save("1", entity1);

        TestEntity entity2 = new TestEntity("1", "updated");
        storage.saveOrUpdate("1", entity2);

        TestEntity retrieved = storage.get(TestEntity.class, "1");
        assertThat(retrieved.getName()).isEqualTo("updated");
    }

    @Test
    void shouldDeleteObject() {
        TestEntity entity = new TestEntity("1", "test");
        storage.save("1", entity);

        storage.delete("1");

        TestEntity retrieved = storage.get(TestEntity.class, "1");
        assertThat(retrieved).isNull();
    }

    @Test
    void shouldReturnNullForNonExistentObject() {
        TestEntity retrieved = storage.get(TestEntity.class, "999");
        assertThat(retrieved).isNull();
    }

    @Test
    void shouldReturnNullForWrongType() {
        TestEntity entity = new TestEntity("1", "test");
        storage.save("1", entity);

        AnotherEntity retrieved = storage.get(AnotherEntity.class, "1");
        assertThat(retrieved).isNull();
    }

    @Test
    void shouldGetLockedObject() {
        TestEntity entity = new TestEntity("1", "test");
        storage.save("1", entity);

        TestEntity retrieved = storage.getLocked(TestEntity.class, "1");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("test");
    }

    @Test
    void shouldThrowExceptionWhenLockingNonExistentObject() {
        assertThatThrownBy(() -> storage.getLocked(TestEntity.class, "999"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not exist");
    }

    @Test
    void shouldThrowExceptionWhenLockingAlreadyLockedObject() {
        TestEntity entity = new TestEntity("1", "test");
        storage.save("1", entity);

        storage.getLocked(TestEntity.class, "1");

        assertThatThrownBy(() -> storage.getLocked(TestEntity.class, "1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already locked");
    }

    @Test
    void shouldReleaseLockWhenDeleted() {
        TestEntity entity = new TestEntity("1", "test");
        storage.save("1", entity);
        storage.getLocked(TestEntity.class, "1");

        storage.delete("1");

        // After delete, we can save and lock again
        storage.save("1", new TestEntity("1", "new"));
        TestEntity retrieved = storage.getLocked(TestEntity.class, "1");
        assertThat(retrieved).isNotNull();
    }

    @Test
    void shouldSaveCollection() {
        List<TestEntity> entities = Arrays.asList(
            new TestEntity("1", "name1"),
            new TestEntity("2", "name2"),
            new TestEntity("3", "name3")
        );

        storage.saveCollection(entities);

        List<TestEntity> all = storage.getAll(TestEntity.class);
        assertThat(all).hasSize(3);
    }

    @Test
    void shouldSaveOrUpdateCollection() {
        List<TestEntity> entities = Arrays.asList(
            new TestEntity("1", "name1"),
            new TestEntity("2", "name2")
        );

        storage.saveOrUpdateCollection(entities);

        List<TestEntity> all = storage.getAll(TestEntity.class);
        assertThat(all).hasSize(2);
    }

    @Test
    void shouldGetAllObjectsOfType() {
        storage.save("1", new TestEntity("1", "name1"));
        storage.save("2", new TestEntity("2", "name2"));
        storage.save("3", new AnotherEntity("3", 100));

        List<TestEntity> entities = storage.getAll(TestEntity.class);

        assertThat(entities).hasSize(2);
        assertThat(entities).extracting("name").containsExactlyInAnyOrder("name1", "name2");
    }

    @Test
    void shouldReturnEmptyListWhenNoObjectsOfType() {
        storage.save("1", new TestEntity("1", "name1"));

        List<AnotherEntity> entities = storage.getAll(AnotherEntity.class);

        assertThat(entities).isEmpty();
    }

    @Test
    void shouldGetUniqueItemByKey() {
        storage.save("1", new TestEntity("id1", "uniqueName"));
        storage.save("2", new TestEntity("id2", "anotherName"));

        TestEntity found = storage.getUniqueItem(TestEntity.class, "name", "uniqueName");

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo("id1");
    }

    @Test
    void shouldReturnNullWhenUniqueItemNotFound() {
        storage.save("1", new TestEntity("id1", "name1"));

        TestEntity found = storage.getUniqueItem(TestEntity.class, "name", "nonExistent");

        assertThat(found).isNull();
    }

    @Test
    void shouldReturnNullWhenFieldDoesNotExist() {
        storage.save("1", new TestEntity("id1", "name1"));

        TestEntity found = storage.getUniqueItem(TestEntity.class, "nonExistentField", "value");

        assertThat(found).isNull();
    }

    @Test
    void shouldIncrementCounter() {
        long value1 = storage.incrCounter("counter1");
        long value2 = storage.incrCounter("counter1");
        long value3 = storage.incrCounter("counter1");

        assertThat(value1).isEqualTo(1);
        assertThat(value2).isEqualTo(2);
        assertThat(value3).isEqualTo(3);
    }

    @Test
    void shouldMaintainSeparateCounters() {
        long counter1_1 = storage.incrCounter("counter1");
        long counter2_1 = storage.incrCounter("counter2");
        long counter1_2 = storage.incrCounter("counter1");

        assertThat(counter1_1).isEqualTo(1);
        assertThat(counter2_1).isEqualTo(1);
        assertThat(counter1_2).isEqualTo(2);
    }

    @Test
    void shouldThrowUnsupportedOperationForMakeClone() {
        TestEntity entity = new TestEntity("1", "test");

        assertThatThrownBy(() -> storage.makeClone(entity, null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("makeClone not implemented");
    }

    @Test
    void shouldGetMinimalId() {
        storage.save("10", new TestEntity("10", "name1"));
        storage.save("5", new TestEntity("5", "name2"));
        storage.save("20", new TestEntity("20", "name3"));

        Comparator<Serializable> comparator = (a, b) -> {
            Integer numA = Integer.parseInt(a.toString());
            Integer numB = Integer.parseInt(b.toString());
            return numA.compareTo(numB);
        };

        Serializable minId = storage.getMinimalId(comparator);

        assertThat(minId.toString()).isEqualTo("5");
    }

    @Test
    void shouldReturnNullWhenNoIdsForMinimal() {
        Comparator<Serializable> comparator = Comparator.comparing(Serializable::toString);

        Serializable minId = storage.getMinimalId(comparator);

        assertThat(minId).isNull();
    }

    // Test entity classes

    public static class TestEntity {
        private String id;
        private String name;

        public TestEntity() {
        }

        public TestEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class AnotherEntity {
        private String id;
        private int value;

        public AnotherEntity() {
        }

        public AnotherEntity(String id, int value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}
