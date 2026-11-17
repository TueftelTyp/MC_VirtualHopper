package de.tuefteltyp.virtualhopper.client;

import de.tuefteltyp.virtualhopper.VirtualHopperBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Compact, collapsible settings; vanilla HopperMenu remains responsible for item handling. */
public final class VirtualHopperScreen extends Screen {
    private final BlockPos pos;
    private EditBox name;
    private EditBox target;
    private Button setName;
    private Button setTarget;
    private Button list;
    private String status = "";
    private final int panelWidth = 230;

    public VirtualHopperScreen(BlockPos pos) {
        super(Component.translatable("screen.virtualhopper.title"));
        this.pos = pos;
    }

    @Override protected void init() {
        int x = width / 2 - panelWidth / 2;
        int y = height / 2 - 73;
        name = new EditBox(font, x + 48, y + 42, 176, 18, Component.translatable("screen.virtualhopper.name"));
        name.setMaxLength(32);
        name.setHint(Component.translatable("screen.virtualhopper.name_hint"));
        addRenderableWidget(name);
        target = new EditBox(font, x + 48, y + 66, 176, 18, Component.translatable("screen.virtualhopper.target"));
        target.setMaxLength(32);
        target.setHint(Component.translatable("screen.virtualhopper.target_hint"));
        addRenderableWidget(target);
        // Block-entity update packets carry name and target to the client.
        if (minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof VirtualHopperBlockEntity hopper) {
            name.setValue(hopper.getHopperName());
            target.setValue(hopper.getTargetName());
        }
        setName = addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.set_name"), b -> {
            String value = name.getValue().trim();
            if (!value.matches("[a-zA-Z0-9_-]{1,32}")) {
                status = "screen.virtualhopper.invalid";
                return;
            }
            send("virtualhopper name " + coordinates() + " " + value);
            status = "screen.virtualhopper.sent";
        }).bounds(x + 6, y + 93, 105, 20).build());
        setTarget = addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.set_target"), b -> {
            String value = target.getValue().trim();
            if (value.isEmpty()) value = "none";
            if (!value.matches("[a-zA-Z0-9_-]{1,32}")) {
                status = "screen.virtualhopper.invalid";
                return;
            }
            send("virtualhopper target " + coordinates() + " " + value);
            status = "screen.virtualhopper.sent";
        }).bounds(x + 119, y + 93, 105, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.filter.open"), b -> {
            if (minecraft != null) minecraft.gui.setScreen(new VirtualHopperFilterScreen(pos));
        }).bounds(x + 6, y + 173, 218, 20).build());
        list = addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.list"), b -> {
            send("virtualhopper list");
            onClose();
        }).bounds(x + 6, y + 120, 105, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.inventory"), b -> {
            send("virtualhopper open " + coordinates());
            onClose();
        }).bounds(x + 119, y + 120, 105, 20).build());
    }

    private String coordinates() {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private void send(String command) {
        if (minecraft != null && minecraft.getConnection() != null) minecraft.getConnection().sendCommand(command);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = width / 2 - panelWidth / 2;
        int y = height / 2 - 73;
        int bottom = y + 202;
        graphics.fill(x - 8, y - 8, x + panelWidth + 8, bottom, 0xEF0A1722);
        graphics.outline(x - 8, y - 8, panelWidth + 16, bottom - y + 8, 0xFF13D5E5);
        graphics.fill(x - 7, y - 7, x + panelWidth + 7, y - 4, 0xFF16DAE7);
        graphics.text(font, Component.translatable("screen.virtualhopper.title"), x + 6, y - 2, 0xFFAFFBFF, true);
        graphics.text(font, Component.translatable("screen.virtualhopper.name_short"), x + 6, y + 47, 0xFFD2F6FA, false);
        graphics.text(font, Component.translatable("screen.virtualhopper.target_short"), x + 6, y + 71, 0xFFD2F6FA, false);
        graphics.text(font, Component.translatable("screen.virtualhopper.hint"), x + 6, y + 148, 0xFF9AAFB7, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (!status.isBlank()) graphics.text(font, Component.translatable(status), x + 6, y + 161, 0xFFBDF39D, false);
    }
}
