package dev.skydock.client;

import dev.skydock.block.DeviceBlock;
import dev.skydock.menu.DeviceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class DeviceScreen extends AbstractContainerScreen<DeviceMenu> {
    private static final int DARK = 0xFF132C31, TEAL = 0xFF4FA89C, CANVAS = 0xFFE7D6AA, BRASS = 0xFFC69852;
    private Button primaryAction;
    public DeviceScreen(DeviceMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 286; imageHeight = 166; }
    @Override protected void init() {
        super.init();
        if (menu.view.ship() != null && actionable(menu.view.kind()))
            primaryAction = addRenderableWidget(Button.builder(actionLabel(), button -> action()).bounds(leftPos + 24, topPos + 132, imageWidth - 48, 20).build());
    }
    private static boolean actionable(DeviceBlock.Kind kind) { return kind == DeviceBlock.Kind.HELM || kind == DeviceBlock.Kind.CLAMP || kind == DeviceBlock.Kind.SEAT; }
    private Component actionLabel() {
        int flags = menu.flags();
        return Component.literal(switch (menu.view.kind()) {
            case HELM -> (flags & 8) != 0 ? "Release helm (" + SkydockClient.releaseKey() + ")" : "Engage helm";
            case CLAMP -> (flags & 1) != 0 ? "Release mooring clamp" : "Secure mooring clamp";
            case SEAT -> (flags & 32) != 0 ? "Stand up (" + SkydockClient.releaseKey() + ")" : "Take seat";
            default -> "";
        });
    }
    private void action() { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0); }
    @Override protected void containerTick() { super.containerTick(); if (primaryAction != null) primaryAction.setMessage(actionLabel()); }
    @Override protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
        g.fill(leftPos - 2, topPos - 2, leftPos + imageWidth + 2, topPos + imageHeight + 2, BRASS);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, DARK);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + 5, TEAL);
        g.fill(leftPos + 15, topPos + 34, leftPos + imageWidth - 15, topPos + 124, 0xFF1C3A3F);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 16, 13, CANVAS, false);
        String[] paragraphs = lines(); int y = 42;
        for (int paragraph = 0; paragraph < paragraphs.length; paragraph++) {
            for (var line : font.split(Component.literal(paragraphs[paragraph]), imageWidth - 48)) {
                g.drawString(font, line, 24, y, paragraph == 0 ? BRASS : 0xFFFFFFFF, false); y += 12;
            }
            y += 2;
        }
    }
    private String[] lines() {
        var view = menu.view; int flags = menu.flags();
        return switch (view.kind()) {
            case HELM -> new String[] { (flags & 8) != 0 ? "Helm engaged by you" : (flags & 4) != 0 ? "Helm occupied" : "Helm ready",
                    String.format(java.util.Locale.ROOT, "Ship speed: %.1f blocks/s", menu.speed()),
                    (flags & 2) != 0 ? "Cruise control active" : SkydockClient.thrustKeys() + " thrust, " + SkydockClient.turnKeys() + " turn, " + SkydockClient.altitudeKeys() + " altitude",
                    SkydockClient.cruiseKey() + " toggles cruise" };
            case CLAMP -> new String[] { (flags & 1) != 0 ? "Mooring clamp secured" : "Mooring clamp released", "Locks the ship only near its reserved berth", "Securing stops motion and cruise" };
            case SEAT -> new String[] { (flags & 32) != 0 ? "You are seated" : (flags & 16) != 0 ? "Seat occupied" : "Seat available", "Seats carry crew with the moving deck", "Press " + SkydockClient.releaseKey() + " at any time to stand" };
            case LIFT -> new String[] { menu.amount() + (menu.amount() == 1 ? " connected canvas lift cell" : " connected canvas lift cells"), String.format(java.util.Locale.ROOT, "Cluster lift: %.0f kg", menu.value()), "Connect cells on any face to expand." };
            case BALLAST -> new String[] { String.format(java.util.Locale.ROOT, "Block mass: %.0f kg", menu.value()), "Ballast adds stable hull mass", "Ship lift must exceed total mass" };
        };
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) { super.render(g, mouseX, mouseY, partialTick); renderTooltip(g, mouseX, mouseY); }
}
