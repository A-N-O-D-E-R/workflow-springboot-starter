package com.anode.workflow.spring.autoconfigure.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test Spring Boot application for integration testing.
 */
@SpringBootApplication(excludeName = {
    "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
    "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration"
})
public class TestWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestWorkflowApplication.class, args);
    }
}
