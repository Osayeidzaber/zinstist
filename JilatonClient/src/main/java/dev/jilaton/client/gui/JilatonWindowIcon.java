package dev.jilaton.client.gui;

import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Window icon set for the Jilaton mark, read from the mod classpath before any resource pack loads. */
public final class JilatonWindowIcon {
    public static final int[] SIZES = {16, 32, 48, 128, 256};

    private JilatonWindowIcon() { }

    public static List<IoSupplier<InputStream>> suppliers() {
        List<IoSupplier<InputStream>> icons = new ArrayList<>(SIZES.length);
        for (int size : SIZES) icons.add(supplier(size));
        return List.copyOf(icons);
    }

    public static String path(int size) {
        return "/assets/jilaton/brand/icon-" + size + ".png";
    }

    private static IoSupplier<InputStream> supplier(int size) {
        String path = path(size);
        return () -> {
            InputStream stream = JilatonWindowIcon.class.getResourceAsStream(path);
            if (stream == null) throw new IOException("Missing " + path);
            return stream;
        };
    }
}
