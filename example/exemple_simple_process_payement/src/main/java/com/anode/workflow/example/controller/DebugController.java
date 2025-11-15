package com.anode.workflow.example.controller;

import com.anode.workflow.service.runtime.RuntimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DebugController {

    private final List<RuntimeService> services;

    public DebugController(List<RuntimeService> services) {
        this.services = services;
    }

    @GetMapping("/engines")
    public List<String> engines() {
        // Return a list with the count of configured engines
        return services.stream()
            .map(s -> "engine-" + services.indexOf(s))
            .toList();
    }
}
