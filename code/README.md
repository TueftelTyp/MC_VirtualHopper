# VirtualHopper v1.12

Cross-dimensional target fix: destinations are resolved from the persistent name registry even if the target chunk is unloaded. When items are waiting, the mod loads the destination chunk to perform the transfer. This may add server load on large networks; chunks are not kept permanently forced-loaded. Existing names and links are preserved. Back up your world before upgrading.

# VirtualHopper v1.10 (Minecraft 26.3 Fabric)

Author: TueftelTyp

## New default recipe

```
Ender Eye       Redstone Block   Ender Eye
Emerald Block   Hopper           Emerald Block
Diamond Block   Diamond Block    Diamond Block
```

Previous v1.6 recipe: five ender pearls and one hopper (E E / EHE /  E ).

## Mod Menu settings

Install **Mod Menu 21.0.1 for Minecraft 26.3** on the client. Open Mod Menu -> Virtual Hopper -> Configure -> Recipe. Change the nine Minecraft item identifiers (e.g. `minecraft:diamond_block`) and select **Save recipe**. Changes write a *world-specific datapack* at `saves/<world>/datapacks/virtualhopper_recipe`; leave and re-enter the world to activate it. This feature works in **singleplayer only**. On dedicated servers, the owner must manage the recipe datapack directly, since a client must not alter the server's recipe.

The recipe editor currently requires nine non-empty ingredients. To create patterns with blank slots, use a datapack editor. Every slot accepts an item identifier, not a visual item-picker; typing nonexistent identifiers may create an unloadable recipe. The button does not silently promise instant reload.


### General settings in Mod Menu (v1.10)

Open **Mod Menu → Virtual Hopper → Configure → General**. The following options are editable without touching JSON files:

| Setting | Default | Description |
| --- | --- | --- |
| Transfer interval (ticks) | 8 | 1–1200 ticks between transfer attempts. |
| Cross-dimension transfers | Enabled | Permit linked hoppers in different dimensions. |
| Collect dropped items | Enabled | Pick up dropped item entities above the hopper. |

Choose **Save settings** to write `config/virtualhopper.json`. In integrated singleplayer the values take effect immediately. At the main menu they are saved for the next game startup. On dedicated multiplayer servers, these settings belong to the **server**, so Mod Menu disables edits while connected to a remote server. Server operators must edit the server's `config/virtualhopper.json` and restart the server (or otherwise reload the configuration). The **Recipe** button opens the existing world-specific recipe editor.

## Unique names

Every placed hopper starts with a globally reserved random five-digit ID (00000-99999), automatically assigned during its first server tick. You can rename it via the existing settings GUI. Names are reserved across all dimensions and *unloaded* chunks and persisted to `<world>/data/virtualhopper_names.json`. Existing world hoppers are migrated when loaded. The registry is saved atomically, and names are released when their block is broken normally. If all 100000 five-digit IDs are occupied, creating additional unnamed hoppers is not possible until a name is released.

**Migration warning**: Existing v1.6 hoppers which were never loaded after upgrading are registered on their first tick. For absolute confidence when upgrading a world containing duplicated names, make a backup and visit the relevant chunks before renaming or wiring new targets. Unloaded pre-upgrade hoppers cannot be detected from a global index that did not previously exist.

## Build

Java 25 and Gradle 9.8.2:

```
gradle --no-daemon clean build
```

Requires access to Fabric, Mojang and Terraformers Maven to download dependencies. Build and in-game runtime tests are not verified in this environment.

## v1.8 Build fix

- Minecraft 26.x Mod Menu dependency uses `implementation` rather than unavailable `modCompileOnly` configuration.
- Gradle project root name and archive/version identifiers updated to v1.8.
- The gameplay logic, recipe, and unique-name registry are unchanged from v1.7.
- This source archive was not compiled against Minecraft/Fabric locally (Java 25 and Gradle dependencies unavailable here).

## v1.9 UI cleanup (retained)

Removed the redundant "Konfiguration" toggle button from the stand-alone hopper settings screen. Name, destination, and action buttons remain visible. The settings entry point in the regular hopper inventory and shift-right-click shortcut remain unchanged. No transfer or storage logic was changed.

## v1.12 visual adjustment

Reduced Virtual Hopper item model size in first-person, third-person and when dropped on the ground. Also adjusted item frame and inventory presentation. Block appearance and gameplay are unchanged.
