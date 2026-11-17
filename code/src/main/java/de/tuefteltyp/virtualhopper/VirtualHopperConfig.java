package de.tuefteltyp.virtualhopper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Shared server-side gameplay settings. Client edits are supported in integrated singleplayer. */
public final class VirtualHopperConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("virtualhopper.json");
    public int transferIntervalTicks = 8;
    public boolean allowCrossDimension = true;
    public boolean collectDroppedItems = true;
    public static VirtualHopperConfig INSTANCE = new VirtualHopperConfig();

    public static void load() {
        try {
            if (Files.isRegularFile(FILE)) {
                VirtualHopperConfig read = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), VirtualHopperConfig.class);
                if (read != null) INSTANCE = read;
            }
            INSTANCE.transferIntervalTicks = Math.max(1, Math.min(1200, INSTANCE.transferIntervalTicks));
            save();
        } catch (Exception error) {
            System.err.println("[VirtualHopper] Could not load configuration: " + error);
        }
    }

    /** Persist values safely: failed writes do not overwrite an existing config. */
    public static synchronized boolean save() {
        Path temporary = null;
        try {
            INSTANCE.transferIntervalTicks = Math.max(1, Math.min(1200, INSTANCE.transferIntervalTicks));
            Files.createDirectories(FILE.getParent());
            temporary = Files.createTempFile(FILE.getParent(), "virtualhopper-", ".tmp");
            Files.writeString(temporary, GSON.toJson(INSTANCE), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException notAtomic) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception error) {
            System.err.println("[VirtualHopper] Could not save configuration: " + error);
            return false;
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (Exception ignored) { /* Retain original error. */ }
            }
        }
    }
}
