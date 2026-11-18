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
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("stunslamtrainer.json");
    static Config config = new Config();
    static boolean enabled = false;
    static boolean axeActive = false;
    static boolean maceActive = false;
    static boolean axeHit = false;
    static boolean wasAirborne = false;
    static boolean armed = false;
    static boolean hadShieldAtHit = false;
    static int waitForBreak = 0;
    static PlayerEntity target;
    static boolean sawBlocking;
    static KeyBinding toggle, settings;

    public static class Config { public int axeSlot = 1, maceSlot = 2; }
    static void load() { try { if (Files.exists(CONFIG_PATH)) config = GSON.fromJson(Files.readString(CONFIG_PATH), Config.class); } catch (Exception ignored) {} if(config==null)config=new Config(); config.axeSlot=Math.clamp(config.axeSlot,1,9); config.maceSlot=Math.clamp(config.maceSlot,1,9); }
    static void save() { try { Files.createDirectories(CONFIG_PATH.getParent()); Files.writeString(CONFIG_PATH,GSON.toJson(config)); } catch (Exception ignored) {} }

    @Override public void onInitializeClient() {
        load();
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("stunslamtrainer","keys"));
        toggle = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.stunslamtrainer.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, category));
        settings = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.stunslamtrainer.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F4, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while(toggle.wasPressed()) { enabled=!enabled; resetTraining(); if(client.player!=null) client.player.sendMessage(Text.literal("Stun Slam Trainer: "+(enabled?"ON":"OFF")),true); }
            while(settings.wasPressed()) client.setScreen(new SettingsScreen(client.currentScreen));
            if(!enabled || client.player==null || client.world==null) return;
            // A training attempt ends as soon as the player lands.
            if (client.player.isOnGround()) {
                if (wasAirborne || armed) resetTraining();
                return;
            }
            wasAirborne = true;

            // Acquire a shield-holding target when passing through ~4 blocks
            // above it. Keep the axe cue lit throughout the remaining fall.
            if (!armed) {
                for (PlayerEntity other : client.world.getPlayers()) {
                    if (other == client.player || !other.isAlive() || !other.isBlocking()) continue;
                    double vertical = client.player.getY() - other.getY();
                    double horizontalSq = client.player.squaredDistanceTo(other) - vertical * vertical;
                    if (vertical >= 3.85 && vertical <= 4.15 && horizontalSq <= 16.0) {
                        armed = true;
                        axeActive = true;
                        target = other;
                        break;
                    }
                }
            }

            if (waitForBreak > 0) {
                waitForBreak--;
                if (target == null || !target.isAlive()) {
                    waitForBreak = 0;
                } else if (hadShieldAtHit && !target.isBlocking()) {
                    // Best-effort client-side indicator, not confirmed shield disable.
                    maceActive = true;
                    axeActive = false;
                    waitForBreak = 0;
                }
            }
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if(enabled && world.isClient() && player == MinecraftClient.getInstance().player && entity instanceof PlayerEntity other && player.getMainHandStack().getItem() instanceof AxeItem) {
                if (axeActive && !player.isOnGround() && target == other) {
                    // The axe cue disappears immediately after the airborne hit.
                    axeActive = false;
                    axeHit = true;
                    hadShieldAtHit = other.isBlocking();
                    waitForBreak = 12;
                }
            }
            return ActionResult.PASS;
        });
        HudElementRegistry.addLast(Identifier.of("stunslamtrainer","slot_indicator"), (context, tickCounter) -> render(context));
    }
    static void render(DrawContext ctx) {
        MinecraftClient mc=MinecraftClient.getInstance();
        if(!enabled || mc.player==null || mc.options.hudHidden) return;
        boolean mace = maceActive;
        if (!axeActive && !maceActive) return;
        int slot=(mace?config.maceSlot:config.axeSlot)-1;
        // 1px semi-transparent outline, aligned inside the vanilla hotbar slot.
        int x = ctx.getScaledWindowWidth()/2 - 90 + slot*20;
        int y = ctx.getScaledWindowHeight() - 21;
        int color = 0xBFFF4444;
        ctx.fill(x, y, x+20, y+1, color);
        ctx.fill(x, y+19, x+20, y+20, color);
        ctx.fill(x, y+1, x+1, y+19, color);
        ctx.fill(x+19, y+1, x+20, y+19, color);
    }
    private static void resetTraining() {
        axeActive = false;
        maceActive = false;
        axeHit = false;
        wasAirborne = false;
        waitForBreak = 0;
        sawBlocking = false;
        hadShieldAtHit = false;
        armed = false;
        target = null;
    }
}
