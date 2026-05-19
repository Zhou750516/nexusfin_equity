package com.nexusfin.equity.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogErrorFieldsCoverageTest {

    @Test
    void warnAndErrorLogsShouldContainErrorFields() throws IOException {
        Path sourceRoot = Path.of("src/main/java/com/nexusfin/equity");
        List<String> violations = new ArrayList<>();
        try (var paths = Files.walk(sourceRoot)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> scanFile(sourceRoot, path, violations));
        }

        assertThat(violations)
                .as("WARN/ERROR log statements must include errorNo and errorMsg. Violations: %s", violations)
                .isEmpty();
    }

    private void scanFile(Path sourceRoot, Path path, List<String> violations) {
        String content;
        try {
            content = Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
        int searchFrom = 0;
        while (searchFrom < content.length()) {
            int warnIndex = content.indexOf("log.warn(", searchFrom);
            int errorIndex = content.indexOf("log.error(", searchFrom);
            int index = nextIndex(warnIndex, errorIndex);
            if (index < 0) {
                return;
            }
            String statement = extractStatement(content, index);
            if (!statement.contains("errorNo") || !statement.contains("errorMsg")) {
                violations.add(sourceRoot.relativize(path) + ":" + lineNumber(content, index) + " " + firstLine(statement));
            }
            searchFrom = index + Math.max(1, statement.length());
        }
    }

    private int nextIndex(int warnIndex, int errorIndex) {
        if (warnIndex < 0) {
            return errorIndex;
        }
        if (errorIndex < 0) {
            return warnIndex;
        }
        return Math.min(warnIndex, errorIndex);
    }

    private String extractStatement(String content, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < content.length(); index++) {
            char current = content.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
            } else if (current == ';' && depth == 0) {
                return content.substring(start, index + 1);
            }
        }
        return content.substring(start);
    }

    private int lineNumber(String content, int index) {
        int line = 1;
        for (int i = 0; i < index; i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private String firstLine(String statement) {
        return statement.lines().findFirst().orElse(statement).trim();
    }
}
