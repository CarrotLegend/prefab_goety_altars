# Prefab 原生祭坛 GUI 验证

本轮仅修改 GUI 表现层。四物品 ID、14 个普通仪式、三个固定映射、17 张建筑图、材质转换、协议 3、服务端建造及扫描器均保持。本轮开始时两个全仪式图片已存在且已绑定，未恢复上一轮缺图状态。

## 附件 12 项答复

1. 修改前已直接继承 `com.wuest.prefab.structures.gui.GuiStructure`，不是直接继承 Screen。
2. 修改后仍直接继承 GuiStructure，经 GuiBase 使用 Prefab 原生 GUI；不继承 GuiBasicStructure。
3. 使用与 GuiBasicStructure 配置模式相同的背景调用和 GuiBase 原生面板资源。配置模式使用左右面板，不额外叠加单面板 default_background.png；没有复制任何 Prefab 背景。
4. 配置和方向按钮由 createAndAddButton 创建 ExtendedButton；Preview/Build 由 createAndAddCustomButton 创建 CustomButton，Cancel 为 ExtendedButton。文字复用 Prefab 操作语言键。
5. 直接调用 drawControlLeftPanel / drawControlRightPanel；正常尺寸使用 215/117 中心偏移、285 右面板宽和 190 面板高。窄窗口保持左侧配置，缩减右图和底部间距；不修改游戏缩放。
6. Preview 直接调用继承的 performPreview()，仍交给 StructureRenderHandler。
7. Build 仅通过 ModNetwork.CHANNEL 发送本模组 BuildAltarPacket；实际交互手和字段不变。
8. 本模组不设置或使用 configurationEnum，不调用 performCancelOrBuildOrHouseFacing，也不发送 Prefab 默认结构包。GuiStructure 内部的枚举声明属于依赖本身，不复制或修改它。
9. 删除深色 fill 背景/图片底板、白色文字、自定义宽大布局及配置 Button.builder；改用 Prefab 文字工具、面板与按钮工厂。图片仍按真实尺寸等比例显示。
10. 没有删除独立 GUI 资源：当前没有旧祭坛专用背景/按钮 PNG，旧外观由代码绘制。未删除用户图片。
11. 17 张建筑 PNG 和 2 张物品 PNG 全部保留，所有 .gz 也保留。资源及 jar 校验结果见下文。
12. `gradlew.bat clean build --offline --console=plain` 成功（28 秒），16 个单元测试全通过，0 失败/错误/跳过。

## 客户端验收

A：无启示录，英文界面，.cache/native-gui-client-A.log。原生按钮类型、四种逻辑尺寸、按钮无重叠、图片回退及缓存、18 次 GUI 预览/建造全部通过。覆盖 14 个普通仪式及全仪式四方向、主副手、材质独立选择、成功减一和失败不扣。Cancel 关闭且不消耗。

B：启示录 2.3.2 + Ending Library 2.1.15fix，中文界面，.cache/native-gui-client-B.log。26 次预览/网络建造全部通过；四项创造栏内容、14 个普通选项、三个固定物品各四方向、大师锻造独立物品及全部图片显示通过。

逻辑尺寸测试：320×240、427×240、640×360、854×480，resize 保留草稿、按钮保持窗口内且两两不重叠。实际窗口 854×480 在 GUI 缩放 1/2 下截屏。原生背景/按钮资源来自 Prefab jar，不是本模组仿制皮肤。

已查看普通祭坛（缩放 1/2）、全仪式、启示录全仪式及大师锻造截图，英文/中文均无本界面控件重叠。B 中直接打开真实 GuiBasicStructure，等待正常渲染帧后取得 .cache/native-gui-prefab-reference.png，与本模组原生面板截图对照一致；对照过程中没有点击 Prefab Build。原版新手教程浮层与本模组布局无关。

建筑 PNG 图案使用用户原图，完整等比例居中；材质/方向变化不更换图片。配置中空图片和 ResourceManager 缺失/真实解码错误回退有测试，不因图片失败禁用有效结构。复用既有缓存及资源重载监听。

原有 121 种材质组合检查保留。Goety tick 根据 checkCage() 更新祭坛 lit，因此延迟放置检查单独记录这个动态属性，其他方块属性和稳定库存字段仍比较；不宣称实际仪式激活或完整动态 NBT 语义已验证。

## 构建、资源与独立服务器

- clean build：BUILD SUCCESSFUL，日志 .cache/native-gui-clean-build.log；16/16 JUnit 通过。
- 交付：`build/libs/prefab_goety_altars-1.0.0.jar`，4,365,765 字节；SHA-256：`7b656781a87500e2001a6b95131f3cf913aff83216e6878231ac6855012bd03b`。
- 52 个主资源文件修改前后完全一致。19 张 PNG（17 建筑、2 物品）及 17 份 .gz 在源码与最终 jar 中的哈希均与本轮修改前一致，共 36 项。资源基线 .cache/native-gui-hashes.json；审计 .cache/native-gui-final-audit.json。
- jar 无重复条目，保留 5 个物品模型，没有复制 assets/prefab 或 com/wuest 类，没有 smoke/test 类。Goety 依赖范围仍为 [2.5.52.4,)，其他依赖和协议没有修改。
- 源码对比确认，生产 Java 只有 GoetyAltarScreen.java 改变；配置、网络、服务端建造、材质和注册源码均与本轮开始前相同。
- runServer：进入 forgeserveruserdev / Env=SERVER，Minecraft Main 提示必须同意 EULA 后退出；日志 .cache/native-gui-server.log。到达阶段未出现本模组客户端类加载错误，**未完成独立服务器世界启动，不宣称完整 Dedicated Server 验证通过**。
- eula=false 保持不变，文件 SHA-256 仍为 32529f78fed61fc4c050565bb588e1a266482cf6b0d0254a6be88caee187ee25。

## 修改范围

生产 Java 仅 GoetyAltarScreen.java，未新增类。测试修改 RitualDefinitionTest、PreviewSmokeChecks、AltarSmokeChecks、ClientSmokeChecks。完整路径见 CHANGELOG-NATIVE-GUI.md。没有修改 JSON、PNG、.gz 或依赖配置。

隔离测试世界位于 .cache/altar-integration/run，不改原有存档和普通启动器模组。测试截图只保存在 .cache，不进入发布 jar。
