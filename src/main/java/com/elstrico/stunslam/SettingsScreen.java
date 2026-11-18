package com.elstrico.stunslam;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class SettingsScreen extends Screen {
    private final Screen parent;
    public SettingsScreen(Screen parent) { super(Text.literal("Stun Slam Trainer Settings")); this.parent=parent; }
    @Override protected void init() {
        int cx=width/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Axe Slot: "+StunSlamClient.config.axeSlot), b->{StunSlamClient.config.axeSlot=StunSlamClient.config.axeSlot%9+1;b.setMessage(Text.literal("Axe Slot: "+StunSlamClient.config.axeSlot));StunSlamClient.save();}).dimensions(cx-105,height/2-35,210,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Mace Slot: "+StunSlamClient.config.maceSlot), b->{StunSlamClient.config.maceSlot=StunSlamClient.config.maceSlot%9+1;b.setMessage(Text.literal("Mace Slot: "+StunSlamClient.config.maceSlot));StunSlamClient.save();}).dimensions(cx-105,height/2-5,210,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"),b->close()).dimensions(cx-105,height/2+35,210,20).build());
    }
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        super.render(context,mouseX,mouseY,delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width/2,height/2-65,0xFFFFFF);
    }
    @Override public void close() { StunSlamClient.save(); client.setScreen(parent); }
}
