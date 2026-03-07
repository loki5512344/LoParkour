package dev.loki.loparkour.config;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

/**
 * Automatically adds missing keys from the default (bundled) config
 * into the player's config file on disk, preserving all existing values,
 * comments, and blank lines.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Load the default config from the jar as a list of raw lines.</li>
 *   <li>Load the on-disk config as a flat key→value map.</li>
 *   <li>Walk the default lines; for every key that is missing on disk,
 *       emit the default line (with its comment header) into the output.</li>
 *   <li>Rewrite the on-disk file with the merged result.</li>
 * </ol>
 *
 * Sections listed in {@code ignoredSections} are never touched — their
 * default content is never injected (user manages them freely).
 */
public final class ConfigUpdater {

    private ConfigUpdater() {}

    /**
     * Updates {@code diskFile} by injecting any keys that exist in the
     * bundled default but are absent on disk.
     *
     * @param plugin           the plugin (used to read the bundled resource)
     * @param resourceName     path inside the jar, e.g. {@code "config.yml"}
     * @param diskFile         the on-disk file to update
     * @param ignoredSections  top-level sections whose keys should never be added
     *                         (pass {@code null} or empty list to ignore nothing)
     */
    public static void update(
            @NotNull Plugin plugin,
            @NotNull String resourceName,
            @NotNull File diskFile,
            @Nullable List<String> ignoredSections
    ) throws IOException {

        List<String> defaultLines = readResource(plugin, resourceName);
        if (defaultLines == null) {
            plugin.getLogger().warning("[ConfigUpdater] Resource not found in jar: " + resourceName);
            return;
        }

        // Flat key set of what already exists on disk (dot-separated paths)
        Set<String> diskKeys = flatKeys(diskFile);

        // Ignored section prefixes (e.g. "styles" → skip any path starting with "styles.")
        Set<String> ignored = normalizedIgnored(ignoredSections);

        List<String> diskLines  = readLines(diskFile);
        List<String> outputLines = merge(defaultLines, diskLines, diskKeys, ignored);

        // Only write if something actually changed
        if (!outputLines.equals(diskLines)) {
            writeLines(diskFile, outputLines);
            plugin.getLogger().info("[ConfigUpdater] Updated " + resourceName
                    + " — added " + countNew(diskLines, outputLines) + " new key(s).");
        }
    }

    // ── Merge ─────────────────────────────────────────────────────────────────

    /**
     * Merges default lines into disk lines.
     *
     * <p>Strategy: walk disk lines first, emit them as-is. Then walk default
     * lines; for each key block (comment + key line) whose key is absent on
     * disk and not ignored, append it at the correct indentation level.
     */
    private static List<String> merge(
            List<String> defaultLines,
            List<String> diskLines,
            Set<String> diskKeys,
            Set<String> ignored
    ) {
        // We need to insert missing keys at the right position.
        // Build a copy of disk lines we will extend.
        List<String> result = new ArrayList<>(diskLines);

        // Collect blocks from the default file: each block = list of comment
        // lines + the key line itself.  We track the "path stack" to know the
        // full dotted path of every key.
        Deque<String> pathStack = new ArrayDeque<>();
        List<String> pendingComments = new ArrayList<>();

        for (int i = 0; i < defaultLines.size(); i++) {
            String raw = defaultLines.get(i);
            String trimmed = raw.stripLeading();

            // Blank line → flush pending comments, keep as separator
            if (trimmed.isBlank()) {
                pendingComments.add(raw);
                continue;
            }

            // Comment line → accumulate
            if (trimmed.startsWith("#")) {
                pendingComments.add(raw);
                continue;
            }

            // Key line
            int indent = raw.length() - trimmed.length();
            int depth  = indent / 2; // assume 2-space indentation

            // Pop stack back to current depth
            while (pathStack.size() > depth) pathStack.pollLast();

            // Parse key name (everything before ':')
            String keyPart = trimmed.split(":")[0].trim();
            pathStack.addLast(keyPart);
            String fullPath = String.join(".", pathStack);

            boolean isSection = trimmed.endsWith(":") || trimmed.matches(".*:\\s*$");
            boolean isIgnored = isIgnored(fullPath, ignored);

            if (!isIgnored && !diskKeys.contains(fullPath) && !isSection) {
                // This key is missing on disk — inject it
                // Find the best insertion point: after the last line that
                // belongs to the parent section on disk
                int insertAt = findInsertionPoint(result, pathStack, depth);

                // Insert blank separator + comments + key line
                List<String> block = new ArrayList<>();
                if (insertAt == result.size() || !result.get(Math.max(0, insertAt - 1)).isBlank()) {
                    block.add("");
                }
                block.addAll(pendingComments);
                block.add(raw);
                result.addAll(insertAt, block);
            }

            // Clear pending comments regardless
            pendingComments.clear();
        }

        return result;
    }

    /**
     * Finds the line index where a missing key should be inserted.
     * Inserts after the last line of its parent section.
     */
    private static int findInsertionPoint(List<String> lines, Deque<String> pathStack, int depth) {
        if (depth == 0) {
            // Top-level key → append at end
            return lines.size();
        }

        // Parent path
        List<String> parts = new ArrayList<>(pathStack);
        String parentKey = parts.get(depth - 1);
        int parentIndent = (depth - 1) * 2;

        // Find the parent key line in the result
        int parentLine = -1;
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            int ind = l.length() - l.stripLeading().length();
            if (ind == parentIndent && l.stripLeading().startsWith(parentKey + ":")) {
                parentLine = i;
                break;
            }
        }

        if (parentLine < 0) return lines.size();

        // Find end of parent section: next line with same or lower indent that isn't blank/comment
        for (int i = parentLine + 1; i < lines.size(); i++) {
            String l = lines.get(i);
            if (l.isBlank() || l.stripLeading().startsWith("#")) continue;
            int ind = l.length() - l.stripLeading().length();
            if (ind <= parentIndent) return i;
        }

        return lines.size();
    }

    // ── IO helpers ─────────────────────────────────────────────────────────────

    /** Reads a resource from the jar as a list of lines. Returns null if not found. */
    @Nullable
    private static List<String> readResource(Plugin plugin, String name) {
        try (InputStream in = plugin.getResource(name)) {
            if (in == null) return null;
            return readLines(in);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read resource: " + name, ex);
            return null;
        }
    }

    private static List<String> readLines(File file) throws IOException {
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            return readLines(r);
        }
    }

    private static List<String> readLines(InputStream in) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return readLines(r);
        }
    }

    private static List<String> readLines(BufferedReader reader) throws IOException {
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) lines.add(line);
        return lines;
    }

    private static void writeLines(File file, List<String> lines) throws IOException {
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (int i = 0; i < lines.size(); i++) {
                w.write(lines.get(i));
                if (i < lines.size() - 1) w.newLine();
            }
        }
    }

    // ── Key extraction ─────────────────────────────────────────────────────────

    /**
     * Returns the flat set of dotted-path keys present in the file.
     * Only leaf keys (not sections) are returned.
     */
    private static Set<String> flatKeys(File file) throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>();

        List<String> lines = readLines(file);
        for (String raw : lines) {
            String trimmed = raw.stripLeading();
            if (trimmed.isBlank() || trimmed.startsWith("#")) continue;

            int indent = raw.length() - trimmed.length();
            int depth  = indent / 2;

            while (stack.size() > depth) stack.pollLast();

            if (!trimmed.contains(":")) continue;
            String keyPart = trimmed.split(":")[0].trim();
            stack.addLast(keyPart);

            boolean isSection = trimmed.matches("[^:]+:\\s*$");
            if (!isSection) {
                keys.add(String.join(".", stack));
            }
        }
        return keys;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static Set<String> normalizedIgnored(@Nullable List<String> list) {
        if (list == null || list.isEmpty()) return Collections.emptySet();
        Set<String> result = new LinkedHashSet<>();
        for (String s : list) result.add(s.toLowerCase(Locale.ROOT).trim());
        return result;
    }

    private static boolean isIgnored(String fullPath, Set<String> ignored) {
        String lower = fullPath.toLowerCase(Locale.ROOT);
        for (String prefix : ignored) {
            if (lower.equals(prefix) || lower.startsWith(prefix + ".")) return true;
        }
        return false;
    }

    private static int countNew(List<String> before, List<String> after) {
        return Math.max(0, after.size() - before.size());
    }
}
