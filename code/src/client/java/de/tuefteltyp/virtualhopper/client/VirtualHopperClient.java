package de.tuefteltyp.virtualhopper.client;

import de.tuefteltyp.virtualhopper.VirtualHopper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;

/** Keeps vanilla slot interaction intact and adds an obvious settings entry point. */
public final class VirtualHopperClient implements ClientModInitializer {
    private static BlockPos lastInteractedHopper;

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide() || !level.getBlockState(hit.getBlockPos()).is(VirtualHopper.BLOCK)) {
                return InteractionResult.PASS;
            }
            lastInteractedHopper = hit.getBlockPos().immutable();
            return InteractionResult.PASS;
        });

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof HopperScreen) || lastInteractedHopper == null || client.level == null) return;
            // The title now also contains the hopper's unique name. Accept both
            // the legacy unnamed title and the new translated, named title.
            String title = screen.getTitle().getString();
            String baseTitle = Component.translatable("container.virtualhopper.virtual_hopper").getString();
            if (!title.equals(baseTitle) && !title.startsWith(baseTitle + " – ")) return;
            BlockPos pos = lastInteractedHopper;
            if (!client.level.getBlockState(pos).is(VirtualHopper.BLOCK)) return;
            // On the side of the vanilla hopper window: never covers a slot.
            int guiLeft = (width - 176) / 2;
            int guiTop = (height - 133) / 2;
            Button config = Button.builder(Component.translatable("screen.virtualhopper.settings_button"),
                    button -> client.gui.setScreen(new VirtualHopperScreen(pos)))
                    .bounds(guiLeft + 179, guiTop + 4, 72, 20).build();
            Screens.getWidgets(screen).add(config);
        });
    }
}
