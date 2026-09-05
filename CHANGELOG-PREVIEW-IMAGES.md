# 绑定现有 GUI 图片的修改清单

修改生产 Java（3）：
- src/main/java/com/prefabgoetyaltars/structure/RitualDefinition.java：可空图片字段、兼容构造、14 组明确映射。
- src/main/java/com/prefabgoetyaltars/structure/AltarPrefabType.java：固定类型持有同一种定义，大师锻造绑定图片，两个全仪式图片为空。
- src/main/java/com/prefabgoetyaltars/client/screen/GoetyAltarScreen.java：左右分栏、按实际尺寸等比例完整显示、独立图片加载与回退。

新增生产 Java（1）：
- src/main/java/com/prefabgoetyaltars/client/RitualPreviewImages.java：仅客户端的尺寸/失败缓存及资源重载监听。

修改测试 Java（2）：
- src/test/java/com/prefabgoetyaltars/structure/RitualDefinitionTest.java：17 定义、15 图片及旧构造兼容。
- src/smoke/java/com/prefabgoetyaltars/smoke/AltarSmokeChecks.java：真实图片断言、GUI 缩放截屏，并单独记录 Goety 原生 lit 动态变化。

新增测试 Java（1）：
- src/smoke/java/com/prefabgoetyaltars/smoke/PreviewSmokeChecks.java：切换、窗口、等比例、null/缺失/损坏图片、失败缓存和失效检查。

文档：修改 README.md、VERIFICATION.md、FILES.md；新增本文件和 PREVIEW-MAPPING.md。

没有修改 JSON、item model、lang、任何 PNG、.gz、注册 ID、依赖、网络协议、配置格式、材质替换、建造流程或扫描器。原生图像绘制使用 Prefab GuiUtils，世界预览仍使用 performPreview / StructureRenderHandler。
