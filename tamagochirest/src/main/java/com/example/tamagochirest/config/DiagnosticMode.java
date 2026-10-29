package com.example.tamagochirest.config;

import java.util.concurrent.atomic.AtomicReference;
import java.time.Instant;

public class DiagnosticMode {
    public enum Mode {
        NORMAL,
        SLOW,
        FAIL
    }

    private final AtomicReference<ModeState> state = new AtomicReference<>(new ModeState(Mode.NORMAL, null));

    public Mode getCurrentMode() {
        ModeState current = state.get();
        if (current.expiresAt != null && Instant.now().isAfter(current.expiresAt)) {
            state.compareAndSet(current, new ModeState(Mode.NORMAL, null));
            return Mode.NORMAL;
        }
        return current.mode;
    }

    public void setMode(Mode mode, int seconds) {
        Instant expiresAt = (seconds > 0 && mode != Mode.NORMAL)
            ? Instant.now().plusSeconds(seconds)
            : null;
        state.set(new ModeState(mode, expiresAt));
    }

    private static class ModeState {
        final Mode mode;
        final Instant expiresAt;

        ModeState(Mode mode, Instant expiresAt) {
            this.mode = mode;
            this.expiresAt = expiresAt;
        }
    }
}
