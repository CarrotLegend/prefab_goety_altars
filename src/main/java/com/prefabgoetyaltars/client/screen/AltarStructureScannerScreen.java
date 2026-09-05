package com.prefabgoetyaltars.client.screen;

import com.prefabgoetyaltars.dev.scan.*;
import com.prefabgoetyaltars.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class AltarStructureScannerScreen extends Screen {
    private static final String PREFIX = "scanner.prefab_goety_altars.";
    private static final String[] LABELS = {"left", "forward", "down", "length", "width", "height"};
    private final BlockPos scannerPos;
    private final net.minecraft.client.multiplayer.ClientLevel openedLevel;
    private final AltarStructureScannerConfig original;
    private final String[] values = new String[6];
    private String name;
    private boolean submitted;
    private EditBox nameBox;
    private Button scanButton;
    private Button saveButton;
    private int panelX;

    public AltarStructureScannerScreen(BlockPos pos, AltarStructureScannerConfig config) {
        super(Component.translatable(PREFIX + "title"));
        scannerPos = pos.immutable(); openedLevel = Minecraft.getInstance().level; original = config.copy(); name = config.structureZipName;
        int[] numbers = {config.blocksToTheLeft, config.blocksParallel, config.blocksDown, config.blocksLong, config.blocksWide, config.blocksTall};
        for (int i = 0; i < 6; i++) values[i] = Integer.toString(numbers[i]);
    }

    @Override protected void init() {
        panelX = Math.max(4, width - 314);
        for (int i = 0; i < 6; i++) {
            final int index = i;
            int y = 30 + (i % 3) * 25;
            int x = panelX + (i / 3) * 150;
            var field = new EditBox(font, x + 77, y, 39, 20, Component.translatable(PREFIX + LABELS[i]));
            field.setMaxLength(5); field.setValue(values[i]);
            field.setFilter(s -> s.matches("-?[0-9]*"));
            field.setResponder(s -> values[index] = s);
            addRenderableWidget(field);
            addRenderableWidget(Button.builder(Component.literal("−"), b -> step(field, index, -1)).bounds(x + 55, y, 18, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> step(field, index, 1)).bounds(x + 120, y, 18, 20).build());
        }
        nameBox = new EditBox(font, panelX + 105, 112, 190, 20, Component.translatable(PREFIX + "name"));
        nameBox.setMaxLength(128); nameBox.setValue(name); nameBox.setResponder(s -> name = s); addRenderableWidget(nameBox);
        scanButton = addRenderableWidget(Button.builder(Component.translatable(PREFIX + "scan"), b -> submit(true)).bounds(panelX + 6, 207, 120, 20).build());
        saveButton = addRenderableWidget(Button.builder(Component.translatable(PREFIX + "save_close"), b -> onClose()).bounds(panelX + 132, 207, 166, 20).build());
    }

    private void step(EditBox field, int index, int delta) {
        try {
            int min = index < 2 ? -128 : index == 2 ? 0 : 1;
            field.setValue(Integer.toString(Math.max(min, Math.min(128, Integer.parseInt(field.getValue()) + delta))));
        } catch (NumberFormatException ignored) { field.setValue(index < 3 ? "0" : "1"); }
    }

    public boolean canInteract() {
        var mc = Minecraft.getInstance(); var player = mc.player;
        return player != null && mc.level != null && mc.level == openedLevel
                && AltarScannerService.withinDistance(player.distanceToSqr(scannerPos.getCenter()))
                && mc.level.hasChunkAt(scannerPos)
                && mc.level.getBlockState(scannerPos).getBlock() instanceof AltarStructureScannerBlock
                && mc.level.getBlockEntity(scannerPos) instanceof AltarStructureScannerBlockEntity;
    }

    private AltarStructureScannerConfig draft() {
        var config = original.copy();
        config.blocksToTheLeft = Integer.parseInt(values[0]); config.blocksParallel = Integer.parseInt(values[1]);
        config.blocksDown = Integer.parseInt(values[2]); config.blocksLong = Integer.parseInt(values[3]);
        config.blocksWide = Integer.parseInt(values[4]); config.blocksTall = Integer.parseInt(values[5]);
        config.structureZipName = name;
        var level = Minecraft.getInstance().level;
        if (level != null && level.getBlockState(scannerPos).hasProperty(AltarStructureScannerBlock.FACING))
            config.direction = level.getBlockState(scannerPos).getValue(AltarStructureScannerBlock.FACING);
        config.blockPos = scannerPos; config.validate(); return config;
    }

    public ScannerBounds previewBounds() {
        try { return ScannerBounds.of(draft()); } catch (IllegalArgumentException e) { return null; }
    }

    private void submit(boolean scan) {
        if (submitted) return;
        if (canInteract()) {
            try {
                var config = draft();
                if (scan) {
                    AltarStructureScannerConfig.normalizeName(name);
                    ModNetwork.CHANNEL.sendToServer(new ScanAltarStructurePacket(scannerPos, config.GetCompoundTag()));
                } else ModNetwork.CHANNEL.sendToServer(new SyncAltarScannerPacket(scannerPos, config.GetCompoundTag()));
            } catch (IllegalArgumentException e) {
                if (minecraft.player != null) minecraft.player.displayClientMessage(e instanceof ScannerException failure ? failure.playerMessage() : Component.translatable(PREFIX + "error.invalid_range"), false);
                if (scan) return;
            }
        }
        submitted = true; minecraft.setScreen(null);
    }

    @Override public void onClose() { submit(false); }
    @Override public void removed() {
        if (!submitted && canInteract()) {
            try { ModNetwork.CHANNEL.sendToServer(new SyncAltarScannerPacket(scannerPos, draft().GetCompoundTag())); }
            catch (IllegalArgumentException ignored) { }
        }
        submitted = true;
    }
    @Override public void tick() { if (!canInteract()) { submitted = true; minecraft.setScreen(null); } }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX, 5, panelX + 305, 234, 0xCE101820);
        graphics.drawString(font, title, panelX + 7, 12, 0xFFFFFF);
        for (int i = 0; i < 6; i++) graphics.drawString(font, Component.translatable(PREFIX + LABELS[i]), panelX + 7 + (i / 3) * 150, 36 + (i % 3) * 25, 0xEEEEEE);
        graphics.drawString(font, Component.translatable(PREFIX + "name"), panelX + 7, 118, 0xFFFFFF);
        saveButton.active = false; scanButton.active = false;
        try {
            var config = draft(); var bounds = ScannerBounds.of(config); saveButton.active = true;
            graphics.drawString(font, Component.translatable(PREFIX + "corners", bounds.corner().toShortString(), bounds.otherCorner().toShortString()), panelX + 7, 155, 0xDDDDDD);
            graphics.drawString(font, Component.translatable(PREFIX + "volume", config.volume(), config.blocksTall + 1), panelX + 7, 170, 0xDDDDDD);
            try {
                graphics.drawString(font, font.plainSubstrByWidth(AltarStructureScannerConfig.normalizeName(name) + ".gz", 290), panelX + 7, 140, 0x77FFBB);
                scanButton.active = canInteract();
            } catch (IllegalArgumentException e) { graphics.drawString(font, Component.translatable(PREFIX + "invalid_name"), panelX + 7, 140, 0xFF7777); }
        } catch (IllegalArgumentException e) { graphics.drawString(font, Component.translatable(PREFIX + "invalid"), panelX + 7, 155, 0xFF7777); }
        graphics.drawString(font, Component.translatable(PREFIX + "offset_hint"), panelX + 7, 187, 0xBBBBBB);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}


