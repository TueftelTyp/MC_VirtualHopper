package de.tuefteltyp.virtualhopper.client;

import de.tuefteltyp.virtualhopper.VirtualHopperBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Settings: a single target editor is intentionally NOT offered here.
 *  All nine targets are configured in VirtualHopperTargetsScreen. */
public final class VirtualHopperScreen extends Screen {
    private final BlockPos pos;
    private EditBox name;
    private String status = "";
    private int configuredTargets;
    private int distributionMode;
    private final int panelWidth = 230;

    public VirtualHopperScreen(BlockPos pos) {
        super(Component.translatable("screen.virtualhopper.title"));
        this.pos = pos;
    }

    private String coordinates() { return pos.getX() + " " + pos.getY() + " " + pos.getZ(); }
    private void send(String command) {
        if (minecraft != null && minecraft.getConnection() != null)
            minecraft.getConnection().sendCommand("virtualhopper " + command);
    }
    @Override protected void init() {
        int x = width / 2 - panelWidth / 2;
        int y = Math.max(10, (height - 170) / 2);
        name = addRenderableWidget(new EditBox(font, x + 48, y + 30, 176, 18,
                Component.translatable("screen.virtualhopper.name")));
        name.setMaxLength(32);
        name.setHint(Component.translatable("screen.virtualhopper.name_hint"));
        configuredTargets = 0;
        if (minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof VirtualHopperBlockEntity hopper) {
            name.setValue(hopper.getHopperName());
            distributionMode = hopper.getDistributionMode();
            for (int i=0; i<9; i++) if (!hopper.getTargetName(i).isBlank()) configuredTargets++;
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.set_name"), b -> {
            String value = name.getValue().trim();
            if (!value.matches("[a-zA-Z0-9_-]{1,32}")) { status = "screen.virtualhopper.invalid"; return; }
            send("name " + coordinates() + " " + value);
            status = "screen.virtualhopper.sent";
        }).bounds(x + 6, y + 59, 218, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.targets.open"), b -> {
            if (minecraft != null) minecraft.gui.setScreen(new VirtualHopperTargetsScreen(pos));
        }).bounds(x + 6, y + 86, 218, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.filter.open"), b -> {
            if (minecraft != null) minecraft.gui.setScreen(new VirtualHopperFilterScreen(pos));
        }).bounds(x + 6, y + 113, 105, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.inventory"), b -> {
            send("open " + coordinates());
            onClose();
        }).bounds(x + 119, y + 113, 105, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.list"), b -> {
            send("list");
            onClose();
        }).bounds(x + 6, y + 140, 218, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = width / 2 - panelWidth / 2;
        int y = Math.max(10, (height - 170) / 2);
        graphics.fill(x - 8, y - 8, x + panelWidth + 8, y + 181, 0xEF0A1722);
        graphics.outline(x - 8, y - 8, panelWidth + 16, 189, 0xFF13D5E5);
        graphics.fill(x - 7, y - 7, x + panelWidth + 7, y - 4, 0xFF16DAE7);
        graphics.text(font, title, x + 6, y - 2, 0xFFAFFBFF, true);
        graphics.text(font, Component.translatable("screen.virtualhopper.name_short"), x + 6, y + 35, 0xFFD2F6FA, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (!status.isBlank()) graphics.text(font, Component.translatable(status), x + 6, y + 166, 0xFFBDF39D, false);
    }
}
