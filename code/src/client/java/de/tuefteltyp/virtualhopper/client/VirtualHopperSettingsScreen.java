package de.tuefteltyp.virtualhopper.client;

import de.tuefteltyp.virtualhopper.VirtualHopperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** General settings tab of the Mod Menu config screen. Dedicated servers retain authority. */
public final class VirtualHopperSettingsScreen extends Screen {
    private final Screen parent;
    private EditBox interval;
    private Button dimensionButton;
    private Button pickupButton;
    private boolean crossDimension;
    private boolean pickup;
    private String noticeKey = "";
    private boolean error;

    public VirtualHopperSettingsScreen(Screen parent) {
        super(Component.translatable("screen.virtualhopper.general.title"));
        this.parent = parent;
    }

    private static Component label(String key, boolean value) {
        return Component.translatable(key).append(Component.literal(": "))
                .append(Component.translatable(value ? "options.on" : "options.off"));
    }

    private boolean remoteServer() {
        Minecraft client = Minecraft.getInstance();
        return client.level != null && client.getSingleplayerServer() == null;
    }

    @Override protected void init() {
        VirtualHopperConfig c = VirtualHopperConfig.INSTANCE;
        crossDimension = c.allowCrossDimension;
        pickup = c.collectDroppedItems;
        final int left = width / 2 - 151;
        final int top = height / 2 - 97;
        interval = new EditBox(font, left + 198, top + 39, 104, 20,
                Component.translatable("screen.virtualhopper.general.interval"));
        interval.setMaxLength(4);
        interval.setValue(Integer.toString(c.transferIntervalTicks));
        addRenderableWidget(interval);
        dimensionButton = addRenderableWidget(Button.builder(
                label("screen.virtualhopper.general.cross_dimension", crossDimension), b -> {
                    crossDimension = !crossDimension;
                    b.setMessage(label("screen.virtualhopper.general.cross_dimension", crossDimension));
                }).bounds(left, top + 72, 302, 20).build());
        pickupButton = addRenderableWidget(Button.builder(
                label("screen.virtualhopper.general.collect_items", pickup), b -> {
                    pickup = !pickup;
                    b.setMessage(label("screen.virtualhopper.general.collect_items", pickup));
                }).bounds(left, top + 99, 302, 20).build());
        Button saveButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.virtualhopper.general.save"), b -> save())
                .bounds(left, top + 145, 145, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.recipe.tab"),
                b -> minecraft.gui.setScreen(new VirtualHopperRecipeScreen(parent)))
                .bounds(left + 157, top + 145, 145, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 157, top + 171, 145, 20).build());
        if (remoteServer()) {
            interval.active = false;
            dimensionButton.active = false;
            pickupButton.active = false;
            saveButton.active = false;
            noticeKey = "screen.virtualhopper.general.remote";
        }
    }

    private void save() {
        if (remoteServer()) {
            noticeKey = "screen.virtualhopper.general.remote";
            error = true;
            return;
        }
        final int ticks;
        try {
            String raw = interval.getValue().strip();
            if (!raw.matches("[0-9]{1,4}")) throw new NumberFormatException("Not a positive integer");
            ticks = Integer.parseInt(raw);
            if (ticks < 1 || ticks > 1200) throw new NumberFormatException("Out of range");
        } catch (NumberFormatException ex) {
            error = true;
            noticeKey = "screen.virtualhopper.general.invalid";
            return;
        }
        VirtualHopperConfig c = VirtualHopperConfig.INSTANCE;
        int oldTicks = c.transferIntervalTicks;
        boolean oldDimension = c.allowCrossDimension;
        boolean oldPickup = c.collectDroppedItems;
        c.transferIntervalTicks = ticks;
        c.allowCrossDimension = crossDimension;
        c.collectDroppedItems = pickup;
        if (VirtualHopperConfig.save()) {
            error = false;
            noticeKey = "screen.virtualhopper.general.saved";
        } else {
            c.transferIntervalTicks = oldTicks;
            c.allowCrossDimension = oldDimension;
            c.collectDroppedItems = oldPickup;
            error = true;
            noticeKey = "screen.virtualhopper.general.failed";
        }
    }

    @Override public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int left = width / 2 - 151, top = height / 2 - 97;
        graphics.fill(left - 10, top - 10, left + 314, top + 210, 0xEA0D1D2A);
        graphics.outline(left - 10, top - 10, 324, 220, 0xFF15C8D8);
        graphics.text(font, title, left, top + 7, 0xFFE5FFFF, true);
        graphics.text(font, Component.translatable("screen.virtualhopper.general.interval"), left, top + 45, 0xFFB5D8DE, false);
        graphics.text(font, Component.translatable("screen.virtualhopper.general.range"), left, top + 126, 0xFF8CAFB7, false);
        if (!noticeKey.isBlank()) graphics.text(font, Component.translatable(noticeKey), left, top + 200,
                error ? 0xFFFF7777 : 0xFFAAEECC, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
