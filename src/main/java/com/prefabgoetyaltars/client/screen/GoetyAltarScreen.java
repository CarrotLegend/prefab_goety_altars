package com.prefabgoetyaltars.client.screen;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.client.RitualPreviewImages;
import com.wuest.prefab.gui.GuiUtils;
import com.prefabgoetyaltars.item.GoetyAltarItem;
import com.prefabgoetyaltars.network.BuildAltarPacket;
import com.prefabgoetyaltars.network.ModNetwork;
import com.prefabgoetyaltars.structure.*;
import com.wuest.prefab.structures.gui.GuiStructure;
import com.wuest.prefab.structures.render.StructureRenderHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import java.io.IOException;
import java.util.List;

public final class GoetyAltarScreen extends GuiStructure {
    private final GoetyAltarItem item;
    private InteractionHand hand;
    private Button directionButton, ritualButton, altarButton, pedestalButton;
    private List<RitualDefinition> rituals = List.of();
    private int ritualIndex;
    private boolean missingImage;
    private int optionTop, layoutWidth;
    private RitualDefinition activeDefinition;
    private RitualPreviewImages.Size imageSize;
    private int imageGeneration = -1;

    public GoetyAltarScreen(GoetyAltarItem item) { super("Prefab - Goety Ritual Altars"); this.item = item; }
    public void prepare(InteractionHand hand) {
        this.hand = hand; ticksWithScreenOpen = 0; configuration = null; selectedStructure = null; ritualIndex = 0; activeDefinition = null; imageSize = null; structureImageLocation = null;
    }
    private GoetyAltarConfiguration config() { return (GoetyAltarConfiguration) configuration; }
    @Override protected void Initialize() {
        super.Initialize(); clearWidgets();
        layoutWidth = Math.min(430, Math.max(304, width - 16));
        modifiedInitialXAxis = layoutWidth / 2; modifiedInitialYAxis = 117;
        imagePanelWidth = layoutWidth - 145; imagePanelHeight = 190;
        if (hand == null || pos == null || player.getItemInHand(hand).getItem() != item) { closeScreen(); return; }
        rituals = RitualDefinition.available();
        if (configuration == null) {
            String ritual = rituals.isEmpty() ? "unavailable" : rituals.get(0).id();
            configuration = new GoetyAltarConfiguration(item.prefabType(), ritual, AltarMaterialVariant.SHADE_STONE,
                    AltarMaterialVariant.SHADE_STONE, pos, houseFacing);
        } else houseFacing = configuration.houseFacing;
        ritualIndex = 0;
        for (int i = 0; i < rituals.size(); i++) if (rituals.get(i).id().equals(config().ritualId())) ritualIndex = i;
        var xy = getAdjustedXYValue(); int x = xy.getFirst(), y = xy.getSecond();
        optionTop = item.prefabType() == AltarPrefabType.RITUAL ? 80 : 62;
        ritualButton = null;
        if (item.prefabType() == AltarPrefabType.RITUAL) {
            ritualButton = createAndAddButton(x + 8, y + 45, 120, 20, ritualText().getString(), false);
            ritualButton.active = !rituals.isEmpty();
        }
        altarButton = createAndAddButton(x + 8, y + optionTop + 10, 120, 20, materialText(config().altarVariant()).getString(), false);
        pedestalButton = createAndAddButton(x + 8, y + optionTop + 55, 120, 20, materialText(config().pedestalVariant()).getString(), false);
        directionButton = createAndAddButton(x + 136 + (imagePanelWidth - 120) / 2, y + 151, 120, 20, directionText().getString(), false);
        if (layoutWidth == 430) {
            btnVisualize = createAndAddCustomButton(x + 24, y + 177, 90, 20, "prefab.gui.button.preview");
            btnCancel = createAndAddButton(x + 154, y + 177, 90, 20, "prefab.gui.button.cancel");
            btnBuild = createAndAddCustomButton(x + 310, y + 177, 90, 20, "prefab.gui.button.build");
        } else {
            btnVisualize = createAndAddCustomButton(x + 12, y + 177, 90, 20, "prefab.gui.button.preview");
            btnCancel = createAndAddButton(x + (layoutWidth - 90) / 2, y + 177, 90, 20, "prefab.gui.button.cancel");
            btnBuild = createAndAddCustomButton(x + layoutWidth - 102, y + 177, 90, 20, "prefab.gui.button.build");
        }
        refreshStructure();
    }
    private void update(String ritual, AltarMaterialVariant altar, AltarMaterialVariant pedestal) {
        configuration = new GoetyAltarConfiguration(item.prefabType(), ritual, altar, pedestal, configuration.pos, configuration.houseFacing);
        refreshStructure();
    }
    private void refreshStructure() {
        selectedStructure = null; activeDefinition = null; structureImageLocation = null; imageSize = null; missingImage = true;
        try {
            var definition = config().definition();
            activeDefinition = definition;
            var fresh = GoetyAltarStructure.load(definition);
            AltarMaterialReplacement.apply(fresh, config());
            selectedStructure = fresh;
        } catch (IOException | RuntimeException e) {
            PrefabGoetyAltars.LOGGER.warn("Unable to load ritual preview type={} ritual={}", item.prefabType().id(), config().ritualId(), e);
        }
        refreshImage();
        if (btnBuild != null) btnBuild.active = selectedStructure != null;
        if (btnVisualize != null) { btnVisualize.active = selectedStructure != null; checkVisualizationSetting(); }
    }
    private void refreshImage() {
        structureImageLocation = null; imageSize = null; missingImage = true;
        imageGeneration = RitualPreviewImages.generation();
        if (activeDefinition == null) return;
        var texture = activeDefinition.previewTexture();
        var size = RitualPreviewImages.resolve(getMinecraft().getResourceManager(), texture);
        if (size.isPresent()) { structureImageLocation = texture; imageSize = size.get(); missingImage = false; }
    }
    private Component ritualText() { return rituals.isEmpty() ? Component.translatable("gui.prefab_goety_altars.no_rituals") : Component.translatable(rituals.get(ritualIndex).translationKey()).append(" >"); }
    private Component materialText(AltarMaterialVariant v) { return Component.translatable(v.translationKey()).append(" >"); }
    private Component directionText() { return Component.translatable("gui.prefab_goety_altars.direction", Component.translatable("gui.prefab_goety_altars.direction." + configuration.houseFacing.getSerializedName())); }
    @Override public Component getTitle() { return Component.translatable(item.prefabType().translationKey()); }
    @Override public Component getNarrationMessage() { return getTitle(); }
    private void clearOwnPreview() {
        if (StructureRenderHandler.currentStructure instanceof GoetyAltarStructure) StructureRenderHandler.setStructure(null, null);
    }
    @Override public void buttonClicked(AbstractButton button) {
        if (!button.active) return;
        if (button == ritualButton && !rituals.isEmpty()) {
            ritualIndex = (ritualIndex + 1) % rituals.size();
            update(rituals.get(ritualIndex).id(), config().altarVariant(), config().pedestalVariant());
            ritualButton.setMessage(ritualText());
        } else if (button == altarButton) {
            update(config().ritualId(), config().altarVariant().next(), config().pedestalVariant());
            altarButton.setMessage(materialText(config().altarVariant()));
        } else if (button == pedestalButton) {
            update(config().ritualId(), config().altarVariant(), config().pedestalVariant().next());
            pedestalButton.setMessage(materialText(config().pedestalVariant()));
        } else if (button == directionButton) {
            houseFacing = config().houseFacing.getClockWise(); configuration.houseFacing = houseFacing;
            refreshStructure(); directionButton.setMessage(directionText());
        } else if (button == btnCancel) { clearOwnPreview(); closeScreen(); }
        else if (button == btnBuild && btnBuild.active && player.getItemInHand(hand).getItem() == item) {
            ModNetwork.CHANNEL.sendToServer(BuildAltarPacket.from(config(), hand)); btnBuild.active = false; clearOwnPreview(); closeScreen();
        } else if (button == btnVisualize && btnVisualize.active) performPreview();
    }
    @Override protected void preButtonRender(GuiGraphics g, int x, int y, int mouseX, int mouseY, float tick) {
        if (imageGeneration != RitualPreviewImages.generation()) refreshImage();
        renderBackground(g);
        drawControlLeftPanel(g, x + 2, y + 10, 185, imagePanelHeight);
        drawControlRightPanel(g, x + 136, y + 10, imagePanelWidth, imagePanelHeight);
        if (activeDefinition != null) drawSplitString(g, Component.translatable(activeDefinition.translationKey()).getString(), x + 144, y + 17, imagePanelWidth - 16);
        if (selectedStructure != null && structureImageLocation != null && imageSize != null) {
            var fitted = imageSize.fit(imagePanelWidth - 16, 108);
            shownImageWidth = fitted.width(); shownImageHeight = fitted.height();
            int imageX = x + 136 + (imagePanelWidth - shownImageWidth) / 2;
            int imageY = y + 38 + (108 - shownImageHeight) / 2;
            GuiUtils.bindAndDrawScaledTexture(structureImageLocation, g, imageX, imageY, shownImageWidth, shownImageHeight,
                    imageSize.width(), imageSize.height(), imageSize.width(), imageSize.height());
        }
    }
    @Override protected void postButtonRender(GuiGraphics g, int x, int y, int mouseX, int mouseY, float tick) {
        drawSplitString(g, getTitle().getString(), x + 8, y + 17, 120);
        if (item.prefabType() == AltarPrefabType.RITUAL) drawString(g, Component.translatable("gui.prefab_goety_altars.ritual_type").getString(), x + 8, y + 35, textColor);
        drawString(g, Component.translatable("gui.prefab_goety_altars.altar_type").getString(), x + 8, y + optionTop, textColor);
        drawString(g, Component.translatable("gui.prefab_goety_altars.pedestal_type").getString(), x + 8, y + optionTop + 45, textColor);
        if (selectedStructure == null || missingImage) drawSplitString(g, Component.translatable(selectedStructure == null
                ? "gui.prefab_goety_altars.missing_structure" : "gui.prefab_goety_altars.missing_image").getString(),
                x + 144, y + 40, imagePanelWidth - 16);
    }
}
