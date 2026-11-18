# Virtual Hopper v1.18 QA

## Executed checks
- Java compiler parsing pass (unresolved Minecraft/Fabric dependencies are expected in this environment, and no syntax diagnostics were emitted).
- ItemFilterRulesSmokeTest: all 9 tests passed.
- All JSON assets parsed successfully.
- Source archive ZIP integrity verified.

## Not executed
- Full Gradle/Loom compilation (Java 25 and Minecraft/Fabric dependencies unavailable here).
- Minecraft runtime test.

## Manual test matrix
1. Upgrade a v1.15 save: previous target appears in slot 1.
2. Assign unique targets in slots 1-9; save and reopen; restart world.
3. Verify Round Robin across equally available targets (64 items across four destinations => 16 each when inventories have room).
4. Set slot 2 full; Round Robin skips it without losing items.
5. Priority chooses the first eligible non-full target.
6. Balance by inventory fill chooses an eligible target with fewer total items.
7. Activate independent target filters, including a modded item, blacklist, whitelist and empty whitelist.
8. Verify global pickup and send filters still apply in all modes.
9. Test Nether to Overworld targets and unloaded target chunks.
10. Check named-target uniqueness, removal, and re-adding; ensure no duplicate destinations.
11. Check single-target local handling, vanilla item insertion, recipe and Mod Menu.

## v1.18-specific checks
1. The main settings screen only has one destination management entry.
2. Add holz1 and holz2 in separate numbered slots. Verify the names are both visible and persist on reopening.
3. Use Check destination status: the server must list both slots.
4. Round Robin should alternate between two available targets. A full target should be skipped.
5. Verify target filters and cross-dimensional destinations.

## v1.20: balanced delivery regression
- Two named targets connected; source has 64 coal. Both target hoppers attached to output inventories that drain at different speeds. Balanced mode: both should receive about half rather than favoring the faster-draining hopper.
- /virtualhopper targets x y z should show delivered counts for all registered targets.
- Reload world, confirm counts persist and distribution continues.
- Fill one target to capacity; ensure it is skipped and no items disappear.
- Set a destination filter to exclude coal; confirm coal is not routed there and counters do not increment.
- Verify round robin mode also alternates despite differing target fill levels.


## v1.20: stale-name reclaim

1. Place hopper A and rename to `k2`. Break A in survival.
2. Place hopper B; rename to `k2` while the old chunk is loaded. Expect success.
3. Save and reopen the world; `k2` must still belong to B.
4. Leave hopper C with name `remote` in an unloaded chunk. Rename B to `remote`; expect rejection.
5. Break a hopper with silk touch / creative and retry the released name.
6. Verify that a hopper at the stored position with the same name is never unregistered.

The source-side cleanup remains best-effort: stale names in unloaded chunks are not
reclaimed until their position becomes loaded, to avoid accidental name theft.
