package com.anode.workflow.spring.autoconfigure.properties;

import lombok.Data;

/**
 * SLA configuration properties.
 */
@Data
public class SlaProperties {
    /**
     * Enable SLA queue manager.
     */
    private boolean enabled = false;

    /**
     * Custom SLA queue manager bean name.
     */
    private String queueManagerBean;
}