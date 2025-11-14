package com.anode.workflow.spring.autoconfigure.properties;

import lombok.Data;

/**
 * Event handler configuration properties.
 */
@Data
public class EventHandlerProperties {
    /**
     * Enable default event handler.
     */
    private boolean enabled = true;

    /**
     * Custom event handler bean name.
     */
    private String beanName;
}
