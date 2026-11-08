package dev.kuro.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KuroClient implements ClientModInitializer {
    public static KeyMapping MENU, ZOOM, HUD;

    @Override
    public void onInitializeClient() {
        Config.load();
        Mods.init();
        MENU = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.kuro.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.kuro"));
        ZOOM = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.kuro.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.kuro"));
        HUD = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.kuro.hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.kuro"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (MENU.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new KuroScreen());
            }
            while (HUD.consumeClick()) {
                Mods.hudShow.toggle();
                Config.save();
            }
            for (Mod m : Mods.ALL) m.tick(mc);
        });
        HudRenderCallback.EVENT.register(Hud::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            Mods.zoom.restore(mc);
            Config.save();
        });
    }
}
