package com.anode.workflow.spring.autoconfigure.storage;

import com.anode.tool.service.CommonService;
import com.anode.tool.service.IdFactory;
import com.anode.workflow.storage.WorkflowJson;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * {@link CommonService} over two PostgreSQL tables: {@code (doc_key, kind, content)} JSON documents
 * and {@code (counter_key, value)} counters. Only the operations the engine performs are supported.
 *
 * <p>Each call runs in the caller's transaction when there is one (Spring-managed {@code
 * DataSource}), otherwise in its own.
 */
public class JdbcWorkflowStore implements CommonService {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");

    private final JdbcClient jdbc;
    private final String upsertDocument;
    private final String insertDocument;
    private final String updateDocument;
    private final String selectDocument;
    private final String deleteDocument;
    private final String incrementCounter;

    public JdbcWorkflowStore(JdbcClient jdbc, String documentTable, String counterTable) {
        this.jdbc = jdbc;
        String documents = identifier(documentTable);
        String counters = identifier(counterTable);
        upsertDocument = "INSERT INTO " + documents + " (doc_key, kind, content) VALUES (:key, :kind, :content)"
                + " ON CONFLICT (doc_key) DO UPDATE SET kind = EXCLUDED.kind, content = EXCLUDED.content, updated_at = now()";
        insertDocument = "INSERT INTO " + documents + " (doc_key, kind, content) VALUES (:key, :kind, :content)"
                + " ON CONFLICT (doc_key) DO NOTHING";
        updateDocument = "UPDATE " + documents + " SET kind = :kind, content = :content, updated_at = now() WHERE doc_key = :key";
        selectDocument = "SELECT content FROM " + documents + " WHERE doc_key = :key";
        deleteDocument = "DELETE FROM " + documents + " WHERE doc_key = :key";
        incrementCounter = "INSERT INTO " + counters + " (counter_key, value) VALUES (:key, 1)"
                + " ON CONFLICT (counter_key) DO UPDATE SET value = " + counters + ".value + 1 RETURNING value";
    }

    /** Table names come from configuration but end up in SQL text: accept plain identifiers only. */
    private static String identifier(String name) {
        if (name == null || !IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid table name: " + name);
        }
        return name;
    }

    @Override
    public void saveOrUpdate(Serializable id, Object object) {
        write(upsertDocument, id, object);
    }

    @Override
    public void save(Serializable id, Object object) {
        write(insertDocument, id, object);
    }

    @Override
    public void update(Serializable id, Object object) {
        write(updateDocument, id, object);
    }

    private void write(String sql, Serializable id, Object object) {
        jdbc.sql(sql)
                .param("key", id.toString())
                .param("kind", WorkflowJson.kindOf(object).name())
                .param("content", WorkflowJson.write(object))
                .update();
    }

    @Override
    public <T> T get(Class<T> objectClass, Serializable id) {
        return jdbc.sql(selectDocument).param("key", id.toString()).query(String.class).optional()
                .map(json -> WorkflowJson.read(json, objectClass))
                .orElse(null);
    }

    @Override
    public void delete(Serializable id) {
        jdbc.sql(deleteDocument).param("key", id.toString()).update();
    }

    @Override
    public long incrCounter(String key) {
        return jdbc.sql(incrementCounter).param("key", key).query(Long.class).single();
    }

    @Override
    public <T> T getLocked(Class<T> objectClass, Serializable id) {
        throw unsupported("getLocked: lock the case in your own transaction instead");
    }

    @Override
    public void saveCollection(Collection objects) {
        throw unsupported("saveCollection");
    }

    @Override
    public void saveOrUpdateCollection(Collection objects) {
        throw unsupported("saveOrUpdateCollection");
    }

    @Override
    public <T> List<T> getAll(Class<T> type) {
        throw unsupported("getAll");
    }

    @Override
    public <T> T getUniqueItem(Class<T> type, String uniqueKeyName, String uniqueKeyValue) {
        throw unsupported("getUniqueItem");
    }

    @Override
    public Map<Serializable, Serializable> makeClone(Object object, IdFactory idFactory) {
        throw unsupported("makeClone");
    }

    @Override
    public Serializable getMinimalId(Comparator<Serializable> comparator) {
        throw unsupported("getMinimalId");
    }

    private static UnsupportedOperationException unsupported(String operation) {
        return new UnsupportedOperationException(operation + " is not used by the workflow engine");
    }
}
