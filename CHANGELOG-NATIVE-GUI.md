# Prefab 原生 GUI 修改清单

生产 Java 仅修改：
- src/main/java/com/prefabgoetyaltars/client/screen/GoetyAltarScreen.java：移除深色 fill 面板、白色标签、大面板布局和配置 Button.builder；调用 Prefab 背景/左右 Panel、原生文字工具和控件工厂；统一处理控件事件，保留本模组 Build 和原生 Preview。

测试 Java 修改：
- src/test/java/com/prefabgoetyaltars/structure/RitualDefinitionTest.java：当前 17 张建筑图的真实映射。
- src/smoke/java/com/prefabgoetyaltars/smoke/PreviewSmokeChecks.java：原生控件类型、控件无重叠、窗口和图片回退，以及真实 GuiBasicStructure 对照截图。
- src/smoke/java/com/prefabgoetyaltars/smoke/AltarSmokeChecks.java：Cancel 不消耗，原生 GUI 截图和已有建造回归。
- src/smoke/java/com/prefabgoetyaltars/smoke/ClientSmokeChecks.java：隔离客户端 A 使用英文、B 使用中文，并等待资源重载。

没有新增 Java 类，没有修改或删除任何 JSON、PNG、.gz、模型、依赖或协议。原有两个全仪式图片绑定保留。不存在需要删除的旧独立 GUI 背景/按钮资源，旧界面的深色外观完全由代码绘制。

文档更新：README.md、PREVIEW-MAPPING.md、VERIFICATION.md、FILES.md；新增本文件。
