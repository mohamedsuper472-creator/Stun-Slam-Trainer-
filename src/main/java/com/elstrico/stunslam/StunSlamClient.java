
package com.elstrico.stunslam;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;

public final class StunSlamClient implements ClientModInitializer {

    private static final Gson GSON =
        new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_PATH =
        FabricLoader.getInstance()
            .getConfigDir()
            .resolve("stunslamtrainer.json");

    static Config config = new Config();

    private static boolean enabled = false;
    private static int maceTicks = 0;
    private static int waitForBreak = 0;

    private static PlayerEntity target;
    private static boolean sawBlocking;

    private static KeyBinding toggle;
    private static KeyBinding settings;

    public static class Config {
        public int axeSlot = 1;
        public int maceSlot = 2;
    }

    private static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                config = GSON.fromJson(
                    Files.readString(CONFIG_PATH),
                    Config.class
                );
            }
        } catch (Exception e) {
            System.err.println(
                "Failed to load Stun Slam config: "
                + e.getMessage()
            );
        }

        if (config == null) {
            config = new Config();
        }

        config.axeSlot =
            Math.clamp(config.axeSlot, 1, 9);

        config.maceSlot =
            Math.clamp(config.maceSlot, 1, 9);
    }

    static void save() {
        try {
            Files.createDirectories(
                CONFIG_PATH.getParent()
            );

            Files.writeString(
                CONFIG_PATH,
                GSON.toJson(config)
            );

        } catch (Exception e) {
            System.err.println(
                "Failed to save Stun Slam config: "
                + e.getMessage()
            );
        }
    }

    @Override
    public void onInitializeClient() {

        load();

        KeyBinding.Category category =
            KeyBinding.Category.create(
                Identifier.of(
                    "stunslamtrainer",
                    "keys"
                )
            );

        toggle = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.stunslamtrainer.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                category
            )
        );

        settings = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.stunslamtrainer.settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                category
            )
        );

        ClientTickEvents.END_CLIENT_TICK.register(
            client -> {

                while (toggle.wasPressed()) {

                    enabled = !enabled;

                    maceTicks = 0;
                    waitForBreak = 0;
                    target = null;
                    sawBlocking = false;

                    if (client.player != null) {
                        client.player.sendMessage(
                            Text.literal(
                                "Stun Slam Trainer: "
                                + (enabled ? "ON" : "OFF")
                            ),
                            true
                        );
                    }
                }

                while (settings.wasPressed()) {
                    client.setScreen(
                        new SettingsScreen(
                            client.currentScreen
                        )
                    );
                }

                if (!enabled ||
                    client.player == null ||
                    client.world == null) {
                    return;
                }

                if (maceTicks > 0) {
                    maceTicks--;
                }

                if (waitForBreak > 0) {

                    waitForBreak--;

                    if (target != null &&
                        target.isAlive()) {

                        if (target.isBlocking()) {
                            sawBlocking = true;
                        }

                        if (sawBlocking &&
                            !target.isBlocking()) {

                            maceTicks = 30;
                            waitForBreak = 0;
                            sawBlocking = false;
                        }
                    }
                }
            }
        );

        AttackEntityCallback.EVENT.register(
            (player, world, hand, entity, hit) -> {

                if (
                    enabled &&
                    world.isClient() &&
                    player ==
                        MinecraftClient.getInstance().player &&
                    entity instanceof PlayerEntity other &&
                    player.getMainHandStack()
                        .getItem() instanceof AxeItem
                ) {

                    target = other;
                    sawBlocking = other.isBlocking();
                    waitForBreak = 12;
                }

                return ActionResult.PASS;
            }
        );

        HudElementRegistry.addLast(
            Identifier.of(
                "stunslamtrainer",
                "slot_indicator"
            ),
            (context, tickCounter) ->
                render(context)
        );
    }

    private static void render(DrawContext ctx) {

        MinecraftClient mc =
            MinecraftClient.getInstance();

        if (!enabled ||
            mc.player == null ||
            mc.world == null ||
            mc.options.hudHidden) {
            return;
        }

        boolean mace = maceTicks > 0;

        if (!mace) {

            boolean fourBlocks = false;

            for (PlayerEntity other :
                mc.world.getPlayers()) {

                if (other == mc.player ||
                    !other.isAlive()) {
                    continue;
                }

                double dy =
                    mc.player.getY() - other.getY();

                if (
                    Math.abs(dy - 4.0) <= 0.12 &&
                    mc.player.squaredDistanceTo(other)
                        < 100 &&
                    other.isBlocking()
                ) {

                    fourBlocks = true;
                    break;
                }
            }

            if (!fourBlocks) {
                return;
            }
        }

        int slot =
            (mace
                ? config.maceSlot
                : config.axeSlot) - 1;

        int x =
            ctx.getScaledWindowWidth() / 2
            - 91 + slot * 20;

        int y =
            ctx.getScaledWindowHeight() - 22;

        // Transparent red background
        ctx.fill(
            x, y,
            x + 20, y + 20,
            0x55FF2222
        );

        // Top border
        ctx.fill(
            x, y,
            x + 20, y + 2,
            0xFFFF2222
        );

        // Bottom border
        ctx.fill(
            x, y + 18,
            x + 20, y + 20,
            0xFFFF2222
        );

        // Left border
        ctx.fill(
            x, y,
            x + 2, y + 20,
            0xFFFF2222
        );

        // Right border
        ctx.fill(
            x + 18, y,
            x + 20, y + 20,
            0xFFFF2222
        );
    }
}
