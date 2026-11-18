package de.tuefteltyp.virtualhopper.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** An item-type-only selector. No item is created in a player inventory or consumed. */
public final class VirtualHopperItemPickerScreen extends Screen {
    private static final int COLUMNS = 9;
    private static final int ROWS = 5;
    private static final int PER_PAGE = COLUMNS * ROWS;

    private final Screen parent;
    private final java.util.function.Consumer<String> onPicked;
    private final int filterPage;
    private final int filterSlot;
    private final List<Item> allItems = new ArrayList<>();
    private final List<Item> results = new ArrayList<>();
    private final Button[] buttons = new Button[PER_PAGE];
    private EditBox search;
    private Button previous;
    private Button next;
    private int resultPage;
    private int hoveredIndex = -1;

    public VirtualHopperItemPickerScreen(VirtualHopperFilterScreen parent, int filterPage, int filterSlot) {
        this(parent, filterPage, filterSlot, id -> parent.applySelection(filterPage, filterSlot, id));
    }

    public VirtualHopperItemPickerScreen(Screen parent, java.util.function.Consumer<String> onPicked) {
        this(parent, 0, 0, onPicked);
    }

    private VirtualHopperItemPickerScreen(Screen parent, int filterPage, int filterSlot, java.util.function.Consumer<String> onPicked) {
        super(Component.translatable("screen.virtualhopper.picker.title"));
        this.parent = parent;
        this.onPicked = onPicked;
        this.filterPage = filterPage;
        this.filterSlot = filterSlot;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) allItems.add(item);
        }
        allItems.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
    }

    @Override protected void init() {
        int x = width / 2 - 160;
        int y = height / 2 - 132;
        search = addRenderableWidget(new EditBox(font, x + 8, y + 26, 304, 20,
                Component.translatable("screen.virtualhopper.picker.search")));
        search.setMaxLength(120);
        search.setHint(Component.translatable("screen.virtualhopper.picker.search"));
        search.setResponder(value -> updateResults());
        for (int i = 0; i < PER_PAGE; i++) {
            final int index = i;
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            buttons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> pick(index))
                    .bounds(x + 11 + col * 33, y + 54 + row * 32, 30, 30).build());
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            resultPage = Math.max(0, resultPage - 1);
            refreshPage();
        }).bounds(x + 8, y + 220, 44, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            resultPage = Math.min(maxPage(), resultPage + 1);
            refreshPage();
        }).bounds(x + 58, y + 220, 44, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.picker.cancel"), b -> onClose())
                .bounds(x + 211, y + 220, 101, 20).build());
        updateResults();
    }

    private void updateResults() {
        if (search == null) return;
        String query = search.getValue().strip().toLowerCase(Locale.ROOT);
        results.clear();
        for (Item item : allItems) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (query.isEmpty() || id.toString().contains(query)
                    || new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                results.add(item);
            }
        }
        resultPage = 0;
        refreshPage();
    }

    private int maxPage() {
        return Math.max(0, (results.size() - 1) / PER_PAGE);
    }

    private void refreshPage() {
        for (int i = 0; i < PER_PAGE; i++) {
            buttons[i].active = resultPage * PER_PAGE + i < results.size();
            buttons[i].visible = buttons[i].active;
        }
        previous.active = resultPage > 0;
        next.active = resultPage < maxPage();
    }

    private void pick(int index) {
        int resultIndex = resultPage * PER_PAGE + index;
        if (resultIndex < 0 || resultIndex >= results.size()) return;
        String itemId = BuiltInRegistries.ITEM.getKey(results.get(resultIndex)).toString();
        onPicked.accept(itemId);
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }

    @Override public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = width / 2 - 160;
        int y = height / 2 - 132;
        graphics.fill(x - 8, y - 8, x + 328, y + 261, 0xF0091722);
        graphics.outline(x - 8, y - 8, 336, 269, 0xFF14CFE2);
        graphics.text(font, title, x + 8, y + 7, 0xFFD9FFFF, true);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        hoveredIndex = -1;
        for (int i = 0; i < PER_PAGE; i++) {
            int resultIndex = resultPage * PER_PAGE + i;
            if (resultIndex >= results.size()) break;
            int slotX = x + 11 + (i % COLUMNS) * 33;
            int slotY = y + 54 + (i / COLUMNS) * 32;
            graphics.item(new ItemStack(results.get(resultIndex)), slotX + 7, slotY + 7);
            if (mouseX >= slotX && mouseX < slotX + 30 && mouseY >= slotY && mouseY < slotY + 30)
                hoveredIndex = resultIndex;
        }
        graphics.text(font, Component.literal((resultPage + 1) + " / " + (maxPage() + 1)
                + "  (" + results.size() + ")"), x + 111, y + 226, 0xFFACC2CF, false);
        if (hoveredIndex >= 0 && hoveredIndex < results.size()) {
            Item selectedItem = results.get(hoveredIndex);
            String id = BuiltInRegistries.ITEM.getKey(selectedItem).toString();
            String name = new ItemStack(selectedItem).getHoverName().getString();
            String line = name + " (" + id + ")";
            if (line.length() > 51) line = line.substring(0, 48) + "...";
            graphics.text(font, Component.literal(line), x + 8, y + 206, 0xFFD9FBFF, false);
        } else {
            graphics.text(font, Component.translatable("screen.virtualhopper.picker.help"),
                    x + 8, y + 206, 0xFFACC2CF, false);
        }
    }
}
