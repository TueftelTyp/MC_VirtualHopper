package de.tuefteltyp.virtualhopper.client;

import de.tuefteltyp.virtualhopper.VirtualHopperBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** One target's ghost filter. Destination names are edited only in the target list. */
public final class VirtualHopperTargetFilterScreen extends Screen {
    private final BlockPos pos;
    private final int targetIndex;
    private final String[] templates = new String[9];
    private final Button[] itemButtons = new Button[9];
    private Button enabledButton;
    private Button modeButton;
    private boolean enabled;
    private boolean blacklist;
    private int selectedSlot;
    private String targetName = "";
    private String notice = "";

    public VirtualHopperTargetFilterScreen(BlockPos pos, int targetIndex) {
        super(Component.translatable("screen.virtualhopper.targets.filter"));
        this.pos = pos;
        this.targetIndex = Math.max(0, Math.min(8, targetIndex));
        java.util.Arrays.fill(templates, "");
    }
    private String coords() { return pos.getX() + " " + pos.getY() + " " + pos.getZ(); }
    private void send(String setting, String value) {
        if (minecraft != null && minecraft.getConnection() != null)
            minecraft.getConnection().sendCommand("virtualhopper targetfilter " + coords() + " "
                    + (targetIndex + 1) + " " + setting + " " + value);
    }
    @Override protected void init() {
        if (minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof VirtualHopperBlockEntity hopper) {
            enabled = hopper.isDestinationFilterEnabled(targetIndex);
            blacklist = hopper.isDestinationFilterBlacklist(targetIndex);
            targetName = hopper.getTargetName(targetIndex);
            for (int i=0; i<9; i++) templates[i] = hopper.getDestinationFilterItem(targetIndex,i);
        }
        int w = Math.min(336, width-20);
        int x = (width-w)/2;
        int y = Math.max(8, (height-165)/2);
        enabledButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            enabled = !enabled;
            send("enabled", enabled ? "on" : "off");
            refresh();
        }).bounds(x, y+27, (w-6)/2, 20).build());
        modeButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            blacklist = !blacklist;
            send("mode", blacklist ? "blacklist" : "whitelist");
            refresh();
        }).bounds(x+(w+6)/2, y+27, (w-6)/2, 20).build());
        int itemWidth = (w-8*3)/9;
        for (int i=0; i<9; i++) {
            final int slot=i;
            itemButtons[i] = addRenderableWidget(Button.builder(Component.literal("+"), b -> pick(slot))
                    .bounds(x+i*(itemWidth+3), y+59, itemWidth, 27).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.filter.clear"), b -> {
            templates[selectedSlot] = "";
            send("slot"+(selectedSlot+1),"none");
            refresh();
        }).bounds(x, y+96, (w-6)/2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.filter.back"), b -> onClose())
                .bounds(x+(w+6)/2, y+96, (w-6)/2, 20).build());
        refresh();
    }
    private void pick(int slot) {
        selectedSlot=slot;
        if (minecraft != null) minecraft.gui.setScreen(new VirtualHopperItemPickerScreen(this, id -> {
            templates[slot]=id;
            send("slot"+(slot+1), id);
            notice="screen.virtualhopper.targets.verify";
            refresh();
        }));
    }
    private void refresh() {
        if (enabledButton == null) return;
        enabledButton.setMessage(Component.translatable(enabled ? "screen.virtualhopper.filter.enable_on" : "screen.virtualhopper.filter.enable_off"));
        modeButton.setMessage(Component.translatable(blacklist ? "screen.virtualhopper.filter.blacklist" : "screen.virtualhopper.filter.whitelist"));
        for (int i=0; i<9; i++) itemButtons[i].setMessage(Component.literal(templates[i].isBlank()?"+":""));
    }
    @Override public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(new VirtualHopperTargetsScreen(pos));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        int w = Math.min(336,width-20);
        int x=(width-w)/2;
        int y=Math.max(8,(height-165)/2);
        graphics.fill(x-8,y-8,x+w+8,y+152,0xF0091722);
        graphics.outline(x-8,y-8,w+16,160,0xFF14CFE2);
        graphics.text(font, title, x+4,y+3,0xFFCEFFFF,true);
        graphics.text(font, Component.literal((targetIndex+1)+": "+targetName),x+4,y+16,0xFFD8F9FF,false);
        super.extractRenderState(graphics,mouseX,mouseY,delta);
        int itemWidth=(w-8*3)/9;
        for (int i=0;i<9;i++) {
            if (templates[i].isEmpty()) continue;
            Identifier key=Identifier.tryParse(templates[i]);
            if (key == null) continue;
            var item=BuiltInRegistries.ITEM.getOptional(key);
            if (item.isPresent()) graphics.item(new ItemStack(item.get()),x+((itemWidth+3)*i)+5,y+65);
        }
        if (!notice.isEmpty()) graphics.text(font,Component.translatable(notice),x+4,y+129,0xFFBDF39D,false);
    }
}
