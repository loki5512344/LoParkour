package dev.loki.loparkour.schematic.schem;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class SchemWriter {

    private SchemWriter() {
    }

    public static void save(@NotNull Clipboard clipboard, @NotNull File file) throws IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            format = ClipboardFormats.findByAlias("sponge");
        }
        if (format == null) {
            throw new IOException("No clipboard format for " + file.getName());
        }
        try (FileOutputStream out = new FileOutputStream(file);
             ClipboardWriter writer = format.getWriter(out)) {
            writer.write(clipboard);
        }
    }
}
