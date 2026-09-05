package com.prefabgoetyaltars.smoke;

import com.prefabgoetyaltars.client.RitualPreviewImages;
import com.prefabgoetyaltars.client.screen.GoetyAltarScreen;
import com.prefabgoetyaltars.structure.*;
import com.wuest.prefab.structures.gui.GuiStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.Resource;
import java.util.Optional;

final class PreviewSmokeChecks {
    private static void check(boolean condition, String text) { if (!condition) throw new IllegalStateException(text); }
    private static Object get(Object o, Class<?> type, String name) throws Exception {
        var f = type.getDeclaredField(name); f.setAccessible(true); return f.get(o);
    }
    static void checkImage(GoetyAltarScreen screen) throws Exception {
        var config = (GoetyAltarConfiguration) get(screen, GuiStructure.class, "configuration");
        var expected = config.definition().previewTexture();
        check(java.util.Objects.equals(expected, get(screen, GuiStructure.class, "structureImageLocation")), "image tracks selected definition");
        check((boolean) get(screen, GoetyAltarScreen.class, "missingImage") == (expected == null), "nullable preview fallback");
    }
    private static net.minecraft.world.item.ItemStack referenceMain, referenceOff;
    private static GoetyAltarScreen altarScreen;
    static void openPrefabReference(GoetyAltarScreen screen) {
        var mc = Minecraft.getInstance(); altarScreen = screen;
        referenceMain = mc.player.getMainHandItem(); referenceOff = mc.player.getOffhandItem();
        var referenceItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues().stream()
                .filter(i -> i instanceof com.wuest.prefab.structures.items.ItemBasicStructure basic
                        && basic.structureType.getBaseOption().getSpecificOptions().size() > 1).findFirst().orElseThrow();
        mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(referenceItem));
        mc.player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        var reference = new com.wuest.prefab.structures.gui.GuiBasicStructure();
        reference.pos = new net.minecraft.core.BlockPos(8,80,8); mc.setScreen(reference);
        com.mojang.logging.LogUtils.getLogger().info("PREFAB_NATIVE_REFERENCE_OPEN: {}", net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(referenceItem));
    }
    static GoetyAltarScreen finishPrefabReference() throws Exception {
        var mc = Minecraft.getInstance();
        check(mc.screen instanceof com.wuest.prefab.structures.gui.GuiBasicStructure, "real Prefab reference screen");
        try (var screenshot = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            screenshot.writeToFile(java.nio.file.Path.of(System.getProperty("altar.smokeProject"), ".cache", "native-gui-prefab-reference.png"));
        }
        mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, referenceMain);
        mc.player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, referenceOff);
        mc.setScreen(altarScreen);
        com.mojang.logging.LogUtils.getLogger().info("PREFAB_NATIVE_REFERENCE_OK");
        return altarScreen;
    }
    static void run(GoetyAltarScreen screen) throws Exception {
        var mc = Minecraft.getInstance();
        for (String name : new String[]{"ritualButton", "altarButton", "pedestalButton", "directionButton"}) {
            var widget = get(screen, GoetyAltarScreen.class, name);
            check(widget == null || widget instanceof com.wuest.prefab.gui.controls.ExtendedButton, "native option button " + name);
        }
        check(get(screen, GuiStructure.class, "btnVisualize") instanceof com.wuest.prefab.gui.controls.CustomButton, "native Preview");
        check(get(screen, GuiStructure.class, "btnBuild") instanceof com.wuest.prefab.gui.controls.CustomButton, "native Build");
        check(get(screen, GuiStructure.class, "btnCancel") instanceof com.wuest.prefab.gui.controls.ExtendedButton, "native Cancel");
        var size = new RitualPreviewImages.Size(2560, 1494);
        for (int[] box : new int[][]{{120,118},{394,220},{640,360},{100,300}}) {
            var fitted = size.fit(box[0], box[1]);
            check(fitted.width() <= box[0] && fitted.height() <= box[1], "image contained");
            check(Math.abs((double) fitted.width() / fitted.height() - 2560.0 / 1494) < 0.04, "image aspect ratio");
        }
        var draft = (GoetyAltarConfiguration) get(screen, GuiStructure.class, "configuration");
        var nbt = draft.WriteToCompoundTag(); int originalWidth = screen.width, originalHeight = screen.height;
        for (int[] viewport : new int[][]{{320,240},{427,240},{640,360},{854,480}}) {
            screen.resize(mc, viewport[0], viewport[1]);
            check(nbt.equals(((GoetyAltarConfiguration) get(screen, GuiStructure.class, "configuration")).WriteToCompoundTag()), "resize keeps draft");
            for (var child : screen.children()) if (child instanceof AbstractWidget widget) {
                check(widget.getX() >= 0 && widget.getY() >= 0 && widget.getX() + widget.getWidth() <= screen.width
                        && widget.getY() + widget.getHeight() <= screen.height, "widget inside viewport");
            }
            var widgets = screen.children().stream().filter(c -> c instanceof AbstractWidget).map(c -> (AbstractWidget)c).toList();
            for (int i = 0; i < widgets.size(); i++) for (int j = i + 1; j < widgets.size(); j++) {
                var a = widgets.get(i); var b = widgets.get(j);
                check(a.getX() + a.getWidth() <= b.getX() || b.getX() + b.getWidth() <= a.getX()
                        || a.getY() + a.getHeight() <= b.getY() || b.getY() + b.getHeight() <= a.getY(), "native widgets do not overlap");
            }
            checkImage(screen);
        }
        screen.resize(mc, originalWidth, originalHeight);
        var missing = new ResourceLocation("prefab_goety_altars", "textures/gui/test_missing.png");
        var corrupt = new ResourceLocation("prefab_goety_altars", "textures/gui/test_corrupt.png");
        int[] reads = {0};
        var fake = (ResourceManager) java.lang.reflect.Proxy.newProxyInstance(ResourceManager.class.getClassLoader(), new Class<?>[]{ResourceManager.class}, (proxy, method, args) -> {
            if (method.getName().equals("getResource")) {
                reads[0]++;
                return args[0].equals(corrupt) ? Optional.of(new Resource(null, () -> new java.io.ByteArrayInputStream(new byte[]{1,2,3}))) : Optional.empty();
            }
            throw new UnsupportedOperationException(method.getName());
        });
        RitualPreviewImages.invalidate();
        check(RitualPreviewImages.resolve(fake, null).isEmpty(), "null image");
        for (int i = 0; i < 3; i++) {
            check(RitualPreviewImages.resolve(fake, missing).isEmpty(), "missing image");
            check(RitualPreviewImages.resolve(fake, corrupt).isEmpty(), "corrupt image");
        }
        check(reads[0] == 2, "failure cache avoids repeated reads/logs");
        int generation = RitualPreviewImages.generation(); RitualPreviewImages.invalidate();
        check(RitualPreviewImages.generation() != generation, "resource reload invalidates cache");
        var definitionField = GoetyAltarScreen.class.getDeclaredField("activeDefinition"); definitionField.setAccessible(true);
        var refresh = GoetyAltarScreen.class.getDeclaredMethod("refreshImage"); refresh.setAccessible(true);
        var original = definitionField.get(screen);
        definitionField.set(screen, RitualDefinition.named("test_only", missing, null)); refresh.invoke(screen);
        check((boolean) get(screen, GoetyAltarScreen.class, "missingImage"), "screen falls back for missing resource");
        check(((AbstractWidget) get(screen, GuiStructure.class, "btnBuild")).active && ((AbstractWidget) get(screen, GuiStructure.class, "btnVisualize")).active, "missing PNG does not disable structure actions");
        definitionField.set(screen, RitualDefinition.named("test_only", null)); refresh.invoke(screen);
        check(get(screen, GuiStructure.class, "structureImageLocation") == null, "null image clears old texture");
        definitionField.set(screen, original); refresh.invoke(screen); checkImage(screen);
        com.mojang.logging.LogUtils.getLogger().info("ALTAR_NATIVE_GUI_OK: native controls, nonoverlapping widgets; mappings, material switches, 4 viewport sizes, aspect fit, null/missing/corrupt fallback, cached failures and reload invalidation");
    }
}
