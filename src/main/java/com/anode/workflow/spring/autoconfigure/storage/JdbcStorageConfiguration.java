package com.anode.workflow.spring.autoconfigure.storage;

import com.anode.tool.service.CommonService;
import com.anode.workflow.spring.autoconfigure.condition.ConditionalOnStorageType;
import com.anode.workflow.spring.autoconfigure.properties.JdbcStorageProperties;
import com.anode.workflow.spring.autoconfigure.properties.StorageProperties.StorageType;
import com.anode.workflow.spring.autoconfigure.properties.WorkflowEnginesProperties;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * Durable storage: every object the engine persists is kept as a JSON document (see {@link
 * com.anode.workflow.storage.WorkflowJson}) in a PostgreSQL table, so a case can be resumed by any
 * instance after a restart.
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass(JdbcClient.class)
@ConditionalOnStorageType(StorageType.JDBC)
public class JdbcStorageConfiguration {

    static final String SCHEMA = "com/anode/workflow/spring/storage/schema-postgresql.sql";

    @Bean
    @ConditionalOnMissingBean(name = "jdbcCommonService")
    @ConditionalOnBean(DataSource.class)
    public CommonService jdbcCommonService(DataSource dataSource, WorkflowEnginesProperties properties) {
        JdbcStorageProperties jdbc = properties.getEngines().stream()
                .filter(engine -> engine.getStorage().getType() == StorageType.JDBC)
                .map(engine -> engine.getStorage().getJdbc())
                .findFirst()
                .orElseGet(JdbcStorageProperties::new);
        if (jdbc.isInitializeSchema()) {
            new ResourceDatabasePopulator(new ClassPathResource(SCHEMA)).execute(dataSource);
        }
        return new JdbcWorkflowStore(JdbcClient.create(dataSource), jdbc.getDocumentTable(), jdbc.getCounterTable());
    }
}
