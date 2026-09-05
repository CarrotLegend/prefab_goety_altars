# 预制建筑-诡厄巫法祭坛

Minecraft 1.20.1 / Forge 47.4.20 / Java 17，modid：prefab_goety_altars，版本 1.0.0，MIT。

## 四个预制建筑物品

| Registry ID（命名空间 prefab_goety_altars） | 中文名 | 对应逻辑 |
|---|---|---|
| ritual_altar | 仪式祭坛 | 原有单仪式轮换，14 个普通仪式 |
| all_ritual | 全仪式祭坛 | 固定 all_ritual.gz |
| revelation_all_ritual | 启示录全仪式祭坛 | 固定 revelation_all_ritual.gz，需要启示录 |
| master_forge_ritual | 启示录大师锻造仪式祭坛 | 固定 master_forge_ritual.gz，需要启示录 |

四个物品始终通过 DeferredRegister 注册，继续使用继承 Prefab StructureItem 的 GoetyAltarItem。未安装启示录时创造标签只显示前两项；安装后按表格顺序显示四项。扫描器是另外的物品，不计作这四个祭坛。

示例：`/give @s prefab_goety_altars:master_forge_ritual`。持物品右键方块上表面；普通物品显示仪式选择按钮，固定物品只显示材质选择。祭坛和基座保持独立 11 选 1，默认阴影石，保留四方向、Preview、Build、Cancel。Prefab 预览期间第一次右键取消预览，再次右键打开 GUI。成功仅扣实际交互手一件，创造模式也扣；失败不扣。

## 模型与纹理

四份祭坛模型以 main 目录中的文件为准：

```text
src/main/resources/assets/prefab_goety_altars/models/item/
  ritual_altar.json
  all_ritual.json
  revelation_all_ritual.json
  master_forge_ritual.json
```

父模型均为 minecraft:item/generated，layer0 为 prefab_goety_altars:item/<相同物品 ID>。扫描器模型保留。数据生成器跳过 --existing 已提供的模型，不在 src/generated 输出重复项。

当前已有 ritual_altar.png、all_ritual.png 两张物品贴图；revelation_all_ritual.png、master_forge_ritual.png 物品贴图尚缺。本次未修改物品模型或任何 PNG，未生成占位图或配方。物品贴图与下述建筑预览图分开使用。

## Prefab 原生祭坛界面

GoetyAltarScreen 直接继承 Prefab GuiStructure / GuiBase，使用 Prefab 自己的背景、左右面板、ExtendedButton 和 CustomButton。普通尺寸参考 GuiBasicStructure 的配置布局；窄窗口缩减右侧图片区域及按钮间距，不改变玩家 GUI 缩放。

左侧依次显示仪式、祭坛材质和基座材质，三个固定物品省略仪式选择。右侧显示结构名称和完整等比例建筑图，下面保留方向按钮。底部顺序为 Preview、Cancel、Build，文字与按钮样式均沿用 Prefab。Preview 仍调用原生 performPreview；Build 仍只发送本模组 BuildAltarPacket。

当前 17 份结构均绑定真实建筑图，目录为 `src/main/resources/assets/prefab_goety_altars/textures/gui/structures/`，包括后来补入的 all_ritual.png 和 revelation_all_ritual.png。完整映射见 [PREVIEW-MAPPING.md](PREVIEW-MAPPING.md)。物品图与建筑图分开使用。

切换仪式同步更新定义、结构、名称及图片；材质/方向切换刷新原生世界预览，静态截图不变。窗口调整保留草稿。图片为空、缺失或读取失败时显示文字提示，结构有效仍可 Preview/Build；客户端缓存尺寸和失败结果，资源重载时失效。图片不能绕过启示录依赖门控。

本轮不修改任何 PNG、结构、JSON、注册、依赖、网络协议或扫描器。验证及附件 12 项答复见 [VERIFICATION.md](VERIFICATION.md)，修改清单见 [CHANGELOG-NATIVE-GUI.md](CHANGELOG-NATIVE-GUI.md)。

## 结构与新增仪式

17 份结构仍在 `src/main/resources/assets/prefab_goety_altars/structures/` 根目录，不移动、不重命名、不改写 NBT 或重新压缩。两个全仪式文件当前内容不同，分别按各自固定物品用途使用；以本次资源校验记录为准。

普通轮换列表固定排序：

```text
adept_nether_ritual
animation_ritual
deep_ritual
end_ritual
expert_nether_ritual
forge_ritual
frost_ritual
geoturgy_ritual
magic_ritual
necroturgy_ritual
overgrown_ritual
sabbath_ritual
sky_ritual
storm_ritual
```

新增普通仪式：放入原生 .gz，在 RitualDefinition 固定列表增加定义，并补充 ritual.prefab_goety_altars.<id> 的中英文语言。有建筑截图时显式填写 previewTexture；未提供图片的旧构造方法和 named(id, requiredModId) 默认 null，不会猜测图片路径。不需要新增独立 Item、模型、Screen 或网络包。执行 processResources/build 后重启，使结构进入 classpath。附属轮换定义仍可使用 RevelationAltarRegistration；大师锻造已由 AltarPrefabType 固定解析。

## 保持现有 Prefab 流程

AltarPrefabType 保存四个物品身份及可信结构映射，RitualDefinition 保存普通仪式定义。客户端仍使用 GoetyAltarScreen、ClientProxy.ModGuis、performPreview 和 StructureRenderHandler；服务端仍使用 Structure.CreateInstance / BuildStructure。

两种材质通过已有 AltarMaterialReplacement 在新加载实例上独立转换，BeforeBuilding 后继续原生旋转、清除空间、放置、BlockEntity 和 Entity 处理。预览使用同一个转换 helper。现有 BlockState 和原始 BlockEntity NBT 不做清理。

协议仍为 3，消息格式、实际交互手、配置字段、玩家权限、距离、区块和世界边界检查均不变。固定物品只改变类型 ID 和映射；两个启示录类型均在使用、GUI 和服务端检查依赖。主体完成不保证延迟实体全部处理完，也不承诺失败时世界回滚、实际仪式激活或完整动态 NBT 语义。

## 构建与运行

```bat
gradlew.bat runData
gradlew.bat clean build
gradlew.bat runClient
```

产物：build/libs/prefab_goety_altars-1.0.0.jar。ForgeGradle 6、Gradle 8.8、Mojang official mappings 保持不变。

Prefab 开发依赖 curse.maven:prefab-246550:6065398，运行范围 [1.10.0.1]；Goety 开发依赖 curse.maven:goety-586095:8750818，运行范围 [2.5.52.4,)。两者仍为硬依赖。Curios 开发运行版本为 5.14.1+1.20.1。启示录 goety_revelation 仍为可选依赖，mandatory=false、ordering=AFTER、side=BOTH、范围 [2.3.2,)。不内嵌任何依赖 jar。

隔离测试在 .cache/altar-integration/run 新建世界，不接触原有存档：

```bat
gradlew.bat runClient -Psmoke -PaltarSmoke
gradlew.bat runClient -Psmoke -PaltarSmoke "-PrevelationJar=<GoetyRevelation-2.3.2.jar 完整路径>" "-PendingLibraryJar=<EndingLibrary-1.20.1-2.1.15fix-all.jar 完整路径>"
```

测试源码 src/smoke 不进入发布 jar。本机启示录内嵌 RevelationFix，需要相应 Ending Library；两个参数仅用于显式开发运行。runServer 仍包含 --nogui，不自动修改 EULA。

本轮原生 GUI 结果见 VERIFICATION.md 和 CHANGELOG-NATIVE-GUI.md；此前图片绑定和四物品调整分别记录在 CHANGELOG-PREVIEW-IMAGES.md、CHANGELOG-FOUR-ITEMS.md。

## 祭坛结构扫描器

`prefab_goety_altars:altar_structure_scanner` 的方块、物品及 BlockEntity 始终注册。只有创造标签页的显示取决于 `FMLEnvironment.production`：开发环境显示，普通游戏隐藏。**扫描器功能在普通 Minecraft、整合包和开发环境都可用，不要求 OP 或创造模式。** 无配方，可由有命令权限的玩家或管理员发放：

```mcfunction
/give @s prefab_goety_altars:altar_structure_scanner
```

获得扫描器后，生存玩家和非 OP 玩家也可以使用。服务端检查发送者、世界、已加载目标、正确方块与 BlockEntity、距离扫描器中心不超过 16 格，以及配置和扫描范围。世界读取和 Prefab 扫描仍在服务器主线程运行。

右键打开 GUI；输入仅修改本地草稿。Esc 或“保存并关闭”保存配置，不执行扫描，允许保存空名称草稿。“扫描并关闭”要求有效名称并发送一次扫描请求。离开世界、离开范围或扫描器被移除时关闭 GUI 并清理独立线框，不改变 Prefab 祭坛预览状态。

### 范围及原生格式

扫描器朝向是放置时玩家的水平方向。默认 Left=0、Forward=1、Down=0，三个尺寸为 1。Left/Forward 为 -128～128，负 Left 向右、负 Forward 向后；Down 为 0～128。Length 沿前方，Width 沿右方，Height 沿上方。

**保留 Prefab 包含端点的高度语义：Height=1 扫描两层。** 实际体积为 `Width × Length × (Height + 1)`，不超过 1,000,000；三个尺寸各为 1～128。世界高度、边界及未加载区块检查继续生效。

配置继承 Prefab `StructureScannerConfig` 并使用其 NBT 接口。`ScannerBounds` 保留完整 BuildClear，只对用于另一角计算的克隆 BuildShape 的宽、长减 1。导出仍调用指定 Prefab 的：

```java
Structure.ScanStructure(serverLevel, scannerPos, corner, otherCorner,
        temporaryFile.toString(), clearedSpace, facing, false, false);
```

不记录空气、保留水；BlockState、BlockEntity NBT、Entity NBT 和 GZip JSON 均由 Prefab 实现，不新增 Goety NBT 清理或自定义格式。先写同目录临时文件，完整校验 GZip 和 Prefab Gson 反序列化结果，再以不替换目标的移动操作发布。已有文件明确拒绝覆盖。

### 两种输出目录

通过 `FMLPaths.GAMEDIR.get()` 获取执行扫描一端的实际游戏目录，不依据 production 选择路径，也不写入安装的 jar。

1. 向上找到 `build.gradle`、匹配 modid 的 `gradle.properties`，并确认 `src/main/resources` 已存在时，输出到该资源目录下的 `assets/prefab_goety_altars/structures`。
2. 没有可靠源码目录时，输出到 `<gameDir>/prefab_goety_altars_exports`，自动创建目录。
3. 两种目录均检查真实路径及符号链接/目录联接，拒绝路径逃逸和不可写目录。

本项目普通 `runClient` 的目录：

```text
D:\ModIMade\prefab_goety_altars\src\main\resources\assets\prefab_goety_altars\structures\<name>.gz
```

本机已定位的普通游戏实例预期目录（最终以运行 API 和成功提示为准）：

```text
D:\my gameeeeee\mc\.minecraft\versions\UntilEternity-v0.1\prefab_goety_altars_exports\<name>.gz
```

多人服务器的文件生成在服务器端。成功提示和 LOGGER 都显示实际绝对文件位置。BlockEntity 缺失、错误方块、超距离、非法参数、非法名称、目录错误、区块未加载、超世界范围、已有文件、写入失败及损坏文件分别反馈，不再显示笼统的“不可用或权限不足”。

名称 trim 后按 Locale.ROOT 小写；空白及非法字符替换成下划线并合并。最终仅 `[a-z0-9_]+`、不超过 128 字符，拒绝空名称、纯下划线及 Windows 保留名；不必输入 `.gz` 后缀。

普通游戏导出后，将所需文件复制到本项目 `src/main/resources/assets/prefab_goety_altars/structures/`。开发环境导出后执行 `gradlew.bat processResources` 或 `gradlew.bat build` 并重启客户端，使新文件进入 classpath；Prefab `Structure.CreateInstance` 不能直接读取源码目录中的外部文件。再按上方“新增仪式”补充定义和语言，无需新增独立物品或材质组合文件。

### 自动检查

```bat
gradlew.bat clean build
gradlew.bat runClient -Psmoke -PscannerSmoke
gradlew.bat runClient -Psmoke -PscannerSmoke -PscannerClasspathCheck
gradlew.bat runClient -Psmoke -PscannerSmoke -PscannerSourceSmoke -PscannerClasspathCheck
```

smoke 源集不进入发布 jar。默认检查使用 `.cache/scanner-integration` 模拟项目和新建测试世界；`scannerSourceSmoke` 使用正常 `run` 目录，实际导出到项目源码资源，验证后只删除本轮唯一命名的测试输出，避免打包测试素材。原生 classpath 检查需要先运行一轮生成 `.cache/scanner-fixtures`。测试截图和日志在 `.cache`。
