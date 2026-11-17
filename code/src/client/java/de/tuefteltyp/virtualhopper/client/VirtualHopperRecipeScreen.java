package de.tuefteltyp.virtualhopper.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Changes the recipe via a generated world datapack, never by modifying the mod jar. */
public final class VirtualHopperRecipeScreen extends Screen {
    private static final String[] DEFAULT = {
        "minecraft:ender_eye", "minecraft:redstone_block", "minecraft:ender_eye",
        "minecraft:emerald_block", "minecraft:hopper", "minecraft:emerald_block",
        "minecraft:diamond_block", "minecraft:diamond_block", "minecraft:diamond_block"
    };
    private final Screen parent;
    private final EditBox[] slots = new EditBox[9];
    private String message = "";
    public VirtualHopperRecipeScreen(Screen parent) {
        super(Component.translatable("screen.virtualhopper.recipe.title"));
        this.parent = parent;
    }
    @Override protected void init() {
        int left = width / 2 - 151;
        int top = height / 2 - 106;
        for (int i = 0; i < 9; i++) {
            int row = i / 3, column = i % 3;
            EditBox field = new EditBox(font, left + column * 104, top + 40 + row * 29, 100, 20,
                    Component.literal("Slot " + (i + 1)));
            field.setMaxLength(100);
            field.setValue(DEFAULT[i]);
            slots[i] = addRenderableWidget(field);
        }
        loadExisting();
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.recipe.save"), b -> save())
                .bounds(left, top + 144, 145, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(left + 157, top + 144, 145, 20).build());
    }
    private void loadExisting() {
        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("datapacks/virtualhopper_recipe/data/virtualhopper/recipe/virtual_hopper.json");
        if (!Files.isRegularFile(path)) return;
        try {
            JsonObject existing = com.google.gson.JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject keys = existing.getAsJsonObject("key");
            if (keys == null) return;
            for (int i = 0; i < 9; i++) {
                String key = String.valueOf((char) ('A' + i));
                if (keys.has(key)) slots[i].setValue(keys.get(key).getAsString());
            }
        } catch (Exception ex) {
            System.err.println("[VirtualHopper] Could not read previous recipe: " + ex);
        }
    }
    private void save() {
        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (server == null) {
            message = "screen.virtualhopper.recipe.singleplayer";
            return;
        }
        String[] ids = new String[9];
        for (int i = 0; i < 9; i++) {
            ids[i] = slots[i].getValue().strip().toLowerCase(java.util.Locale.ROOT);
            if (!ids[i].matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || ids[i].contains("..")) {
                message = "screen.virtualhopper.recipe.invalid";
                return;
            }
            try { Identifier.parse(ids[i]); } catch (Exception ex) {
                message = "screen.virtualhopper.recipe.invalid";
                return;
            }
        }
        try {
            JsonObject recipe = new JsonObject();
            recipe.addProperty("type", "minecraft:crafting_shaped");
            recipe.addProperty("category", "redstone");
            JsonArray pattern = new JsonArray();
            pattern.add("ABC"); pattern.add("DEF"); pattern.add("GHI");
            recipe.add("pattern", pattern);
            JsonObject keys = new JsonObject();
            for (int i = 0; i < 9; i++) keys.addProperty(String.valueOf((char)('A' + i)), ids[i]);
            recipe.add("key", keys);
            JsonObject result = new JsonObject();
            result.addProperty("id", "virtualhopper:virtual_hopper");
            result.addProperty("count", 1);
            recipe.add("result", result);
            Path pack = server.getWorldPath(LevelResource.ROOT).resolve("datapacks").resolve("virtualhopper_recipe");
            Path recipeFile = pack.resolve("data/virtualhopper/recipe/virtual_hopper.json");
            Files.createDirectories(recipeFile.getParent());
            String packInfo = "{\"pack\":{\"description\":\"VirtualHopper adjustable recipe\",\"min_format\":[121,0],\"max_format\":121}}";
            Files.writeString(pack.resolve("pack.mcmeta"), packInfo, StandardCharsets.UTF_8);
            Files.writeString(recipeFile, new GsonBuilder().setPrettyPrinting().create().toJson(recipe), StandardCharsets.UTF_8);
            message = "screen.virtualhopper.recipe.saved";
        } catch (Exception ex) {
            System.err.println("[VirtualHopper] Recipe pack could not be written: " + ex);
            message = "screen.virtualhopper.recipe.error";
        }
    }
    @Override public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int left = width / 2 - 151, top = height / 2 - 106;
        graphics.fill(left - 10, top - 10, left + 314, top + 189, 0xEA0D1D2A);
        graphics.outline(left - 10, top - 10, 324, 199, 0xFF15C8D8);
        graphics.text(font, title, left, top + 7, 0xFFE5FFFF, true);
        graphics.text(font, Component.translatable("screen.virtualhopper.recipe.help"), left, top + 26, 0xFFB5D8DE, false);
        if (!message.isBlank()) graphics.text(font, Component.translatable(message), left, top + 173, 0xFFAAEECC, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
