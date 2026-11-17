package de.tuefteltyp.virtualhopper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VirtualHopperConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("virtualhopper.json");
    public int transferIntervalTicks = 8;
    public boolean allowCrossDimension = true;
    public boolean collectDroppedItems = true;
    public static VirtualHopperConfig INSTANCE = new VirtualHopperConfig();
    public static void load() {
        try {
            if (Files.exists(FILE)) {
                VirtualHopperConfig config = GSON.fromJson(Files.readString(FILE), VirtualHopperConfig.class);
                if (config != null) INSTANCE = config;
            }
            INSTANCE.transferIntervalTicks = Math.max(1, Math.min(1200, INSTANCE.transferIntervalTicks));
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(INSTANCE));
        } catch (Exception error) {
            System.err.println("[VirtualHopper] Could not load configuration: " + error.getMessage());
        }
    }
}
