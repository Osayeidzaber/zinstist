package dev.jilaton.client.module.impl;

import java.util.ArrayList;
import java.util.List;

/** Splits a macro into the lines it sends. */
final class MacroText {
    /** Chat messages are capped at 256 characters by the server. */
    static final int MAX_LINE = 256;

    private MacroText() {
    }

    static List<String> lines(String text) {
        List<String> out = new ArrayList<>();
        for (String part : text.split("&&")) {
            String line = part.strip();
            if (line.isEmpty() || line.equals("/")) continue;
            out.add(line.length() > MAX_LINE ? line.substring(0, MAX_LINE) : line);
        }
        return out;
    }
}
