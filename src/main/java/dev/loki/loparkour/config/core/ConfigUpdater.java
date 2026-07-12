package dev.loki.loparkour.config.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Merges a resource-template YAML with the user's existing file,
 * preserving user values while adding any new keys from the template.
 * Uses full YAML paths (e.g. "sql.enabled") to avoid key collisions.
 */
public class ConfigUpdater {

    private final Logger logger;

    private ConfigUpdater(@NotNull Logger logger) {
        this.logger = logger;
    }

    public static void update(
            @NotNull File configFile,
            @NotNull InputStream templateStream,
            @NotNull String expectedVersion,
            @Nullable List<String> ignoredKeys,
            @NotNull Logger logger) throws IOException {

        new ConfigUpdater(logger).performUpdate(configFile, templateStream, ignoredKeys);
    }

    // ── core ──────────────────────────────────────────────────────────────────

    private void performUpdate(
            @NotNull File configFile,
            @NotNull InputStream templateStream,
            @Nullable List<String> ignoredKeys) throws IOException {

        File backup = createBackup(configFile);
        try {
            List<String> merged = merge(templateStream, configFile, ignoredKeys);
            Files.write(configFile.toPath(), merged);
            logger.info("Configuration file synced: " + configFile.getName());
        } catch (Exception e) {
            if (backup != null && backup.exists()) {
                Files.copy(backup.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.severe("Config sync failed, restored backup: " + e.getMessage());
            }
            throw e;
        }
    }

    // ── merge ─────────────────────────────────────────────────────────────────

    @NotNull
    private List<String> merge(
            @NotNull InputStream templateStream,
            @NotNull File existingFile,
            @Nullable List<String> ignoredKeys) throws IOException {

        List<String> templateLines = readLines(templateStream);
        List<String> existingLines = existingFile.exists() ? readLines(existingFile) : List.of();

        Set<String> ignored = normalizeIgnored(ignoredKeys);
        Map<String, String> existingValues = extractFullPathValues(existingLines);

        List<String> result = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();
        int prevIndent = -1;

        for (String line : templateLines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-")) {
                result.add(line);
                continue;
            }

            int colon = trimmed.indexOf(':');
            if (colon == -1) {
                result.add(line);
                continue;
            }

            String leaf = trimmed.substring(0, colon).trim();
            String after = trimmed.substring(colon + 1).trim();
            int indent = indentOf(line);

            while (prevIndent >= 0 && indent <= prevIndent) {
                if (!stack.isEmpty()) {
                    stack.pop();
                }
                prevIndent -= 2;
            }

            String fullPath = stack.isEmpty() ? leaf : String.join(".", stack) + "." + leaf;
            boolean section = after.isEmpty() || after.startsWith("#");

            if (section) {
                stack.push(leaf);
                prevIndent = indent;
                result.add(line);
            } else if (isIgnored(fullPath, ignored)) {
                result.add(line);
                prevIndent = indent;
            } else {
                String existing = existingValues.get(fullPath);
                result.add(existing != null
                        ? line.substring(0, line.indexOf(':') + 1) + " " + existing
                        : line);
                prevIndent = indent;
            }
        }
        return result;
    }

    @NotNull
    private Map<String, String> extractFullPathValues(@NotNull List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        int prevIndent = -1;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-")) {
                continue;
            }

            int colon = trimmed.indexOf(':');
            if (colon == -1) {
                continue;
            }

            String leaf = trimmed.substring(0, colon).trim();
            String after = trimmed.substring(colon + 1).trim();
            int indent = indentOf(line);

            while (prevIndent >= 0 && indent <= prevIndent) {
                if (!stack.isEmpty()) {
                    stack.pop();
                }
                prevIndent -= 2;
            }

            String fullPath = stack.isEmpty() ? leaf : String.join(".", stack) + "." + leaf;

            if (after.isEmpty() || after.startsWith("#")) {
                stack.push(leaf);
            } else {
                values.put(fullPath, after);
            }
            prevIndent = indent;
        }
        return values;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static int indentOf(@NotNull String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    @Nullable
    private File createBackup(@NotNull File file) {
        if (!file.exists()) return null;
        try {
            File bak = new File(file.getParent(), file.getName() + ".bak");
            Files.copy(file.toPath(), bak.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return bak;
        } catch (IOException e) {
            logger.warning("Failed to create backup: " + e.getMessage());
            return null;
        }
    }

    @NotNull
    private static List<String> readLines(@NotNull InputStream is) throws IOException {
        try (var r = new BufferedReader(new InputStreamReader(is))) {
            return r.lines().toList();
        }
    }

    @NotNull
    private static List<String> readLines(@NotNull File file) throws IOException {
        return Files.readAllLines(file.toPath());
    }

    @NotNull
    private static Set<String> normalizeIgnored(@Nullable List<String> keys) {
        if (keys == null) {
            return Set.of();
        }
        Set<String> s = new HashSet<>();
        for (String k : keys) {
            s.add(k.trim().toLowerCase());
        }
        return s;
    }

    private static boolean isIgnored(@NotNull String fullPath, @NotNull Set<String> ignored) {
        String lower = fullPath.toLowerCase();
        for (String ign : ignored) {
            if (lower.equals(ign) || lower.startsWith(ign + ".")) {
                return true;
            }
        }
        return false;
    }
}
