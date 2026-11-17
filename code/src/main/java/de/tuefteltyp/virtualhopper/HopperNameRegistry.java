package de.tuefteltyp.virtualhopper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Server-wide name reservations, including endpoints in unloaded chunks. */
public final class HopperNameRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static Path file;
    private HopperNameRegistry() {}

    public static synchronized void load(MinecraftServer server) {
        VirtualHopper.ENDPOINTS.clear();
        file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("virtualhopper_names.json");
        if (!Files.isRegularFile(file)) return;
        try {
            JsonObject stored = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            if (stored == null) return;
            for (Map.Entry<String, com.google.gson.JsonElement> item : stored.entrySet()) {
                try {
                    JsonObject location = item.getValue().getAsJsonObject();
                    String dimension = location.get("dimension").getAsString();
                    long pos = location.get("position").getAsLong();
                    var dimensionId = net.minecraft.resources.Identifier.parse(dimension);
                    var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId);
                    VirtualHopper.ENDPOINTS.put(item.getKey(), new VirtualHopper.Endpoint(key, BlockPos.of(pos)));
                } catch (RuntimeException badEntry) {
                    System.err.println("[VirtualHopper] Invalid saved name record: " + item.getKey());
                }
            }
        } catch (Exception ex) {
            System.err.println("[VirtualHopper] Failed to load world name registry: " + ex);
            throw new IllegalStateException("Cannot safely load VirtualHopper names", ex);
        }
    }

    public static synchronized void save() {
        if (file == null) return;
        JsonObject names = new JsonObject();
        new TreeMap<>(VirtualHopper.ENDPOINTS).forEach((name, endpoint) -> {
            JsonObject value = new JsonObject();
            value.addProperty("dimension", endpoint.dimension().identifier().toString());
            value.addProperty("position", endpoint.pos().asLong());
            names.add(name, value);
        });
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName().toString() + ".tmp");
            Files.writeString(temp, GSON.toJson(names), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot save VirtualHopper name reservations", ex);
        }
    }

    public static synchronized void registerIfNeeded(VirtualHopperBlockEntity hopper, ServerLevel level) {
        VirtualHopper.Endpoint point = new VirtualHopper.Endpoint(level.dimension(), hopper.getBlockPos().immutable());
        String name = hopper.getHopperName();
        if (!name.isBlank() && Objects.equals(VirtualHopper.ENDPOINTS.get(name), point)) return;
        if (!name.isBlank() && !VirtualHopper.ENDPOINTS.containsKey(name)) {
            VirtualHopper.ENDPOINTS.put(name, point);
            save();
            return;
        }
        // Blank or conflicting names from older installations are migrated automatically.
        for (int attempt = 0; attempt < 200000; attempt++) {
            String candidate = String.format(java.util.Locale.ROOT, "%05d", RANDOM.nextInt(100000));
            if (!VirtualHopper.ENDPOINTS.containsKey(candidate)) {
                VirtualHopper.ENDPOINTS.put(candidate, point);
                save();
                hopper.setHopperName(candidate);
                return;
            }
        }
        for (int i = 0; i < 100000; i++) {
            String candidate = String.format(java.util.Locale.ROOT, "%05d", i);
            if (!VirtualHopper.ENDPOINTS.containsKey(candidate)) {
                VirtualHopper.ENDPOINTS.put(candidate, point);
                save();
                hopper.setHopperName(candidate);
                return;
            }
        }
        throw new IllegalStateException("All 100000 five-digit VirtualHopper names are in use");
    }

    public static synchronized boolean rename(VirtualHopperBlockEntity hopper, String newName) {
        if (hopper.getLevel() == null || !(hopper.getLevel() instanceof ServerLevel level)
                || newName == null || !newName.matches("[a-zA-Z0-9_-]{1,32}")) return false;
        registerIfNeeded(hopper, level);
        VirtualHopper.Endpoint point = new VirtualHopper.Endpoint(level.dimension(), hopper.getBlockPos().immutable());
        VirtualHopper.Endpoint current = VirtualHopper.ENDPOINTS.get(newName);
        if (current != null && !current.equals(point)) return false;
        String oldName = hopper.getHopperName();
        if (oldName.equals(newName)) return true;
        VirtualHopper.ENDPOINTS.remove(oldName, point);
        VirtualHopper.ENDPOINTS.put(newName, point);
        save();
        hopper.setHopperName(newName);
        return true;
    }

    public static synchronized void remove(VirtualHopperBlockEntity hopper) {
        if (hopper.getLevel() == null || hopper.getHopperName().isBlank()) return;
        var point = new VirtualHopper.Endpoint(hopper.getLevel().dimension(), hopper.getBlockPos().immutable());
        if (VirtualHopper.ENDPOINTS.remove(hopper.getHopperName(), point)) save();
    }
}
