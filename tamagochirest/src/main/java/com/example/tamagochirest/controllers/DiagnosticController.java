package com.example.tamagochirest.controllers;

import com.example.tamagochirest.config.DiagnosticMode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/diagnostic")
public class DiagnosticController {

    private final DiagnosticMode diagnosticMode;

    public DiagnosticController(DiagnosticMode diagnosticMode) {
        this.diagnosticMode = diagnosticMode;
    }

    @PostMapping("/mode")
    public ResponseEntity<Map<String, String>> setMode(
            @RequestParam DiagnosticMode.Mode mode,
            @RequestParam(defaultValue = "60") int seconds) {

        if (seconds < 1 || seconds > 120) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "seconds must be between 1 and 120"));
        }

        diagnosticMode.setMode(mode, seconds);
        return ResponseEntity.ok(Map.of(
            "mode", mode.name(),
            "seconds", String.valueOf(seconds)
        ));
    }

    @GetMapping("/mode")
    public ResponseEntity<Map<String, String>> getMode() {
        return ResponseEntity.ok(Map.of(
            "mode", diagnosticMode.getCurrentMode().name()
        ));
    }
}
