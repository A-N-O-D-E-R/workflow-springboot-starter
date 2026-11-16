package com.anode.workflow.spring.autoconfigure.properties;

import lombok.Data;

/** Component factory configuration of an engine. */
@Data
public class FactoryProperties {

    /** Enable component factory auto-configuration. */
    private boolean enabled = true;
}
