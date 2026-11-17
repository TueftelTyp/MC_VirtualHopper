package de.tuefteltyp.virtualhopper.client;

import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;

public final class VirtualHopperModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return VirtualHopperRecipeScreen::new;
    }
}
