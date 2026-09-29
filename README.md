# Prefab - Goety Ritual Altars

Minecraft 1.21.1 / NeoForge 21.1.214 / Java 21，modid `prefab_goety_altars`，版本 `1.0.3`。

## 内容

项目注册三个物品：

| Registry ID | 行为 |
|---|---|
| `ritual_altar` | 选择具体仪式、祭坛材质、基座材质和方向 |
| `all_ritual` | 固定加载 `all_ritual.gz`，只选择两种材质和方向 |
| `altar_structure_scanner` | 打开开发用扫描界面并调用 Prefab 原生扫描导出 |

同时注册 `altar_structure_scanner` 方块、同 ID 方块实体，以及 `altars` 创造标签页。祭坛物品继续继承 Prefab `StructureItem`，预览和建造使用 Prefab 1.0.9 的原生结构 API。客户端到服务端通信已迁移为 NeoForge payload；协议版本为 5，旧版客户端不能连接。

`ritual_altar` 的固定顺序为 14 个 Goety 本体仪式，然后按已安装模组追加条件选项：

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
master_forge_ritual   （休眠兼容，等待 goety_revelation 的 1.21.1 移植）
culinary_ritual        （需要 goetydelight 1.4.7 或更高版本）
```

GUI 和服务端都会重新检查条件仪式的依赖。`all_ritual` 始终可用；加载独立结构实例后，会过滤当前未安装的可选附属模组方块和对应方块实体，再执行材质转换。源 `.gz` 和 NBT 不会被改写。

## 依赖

- NeoForge 21.1.214
- Prefab NeoForge 1.0.9 或更高版本
- Goety NeoForge 3.1.5.1 或更高版本
- Goety's Delight NeoForge 1.4.7 或更高版本（可选，modid `goetydelight`）

Goety Revelation 当前没有 Minecraft 1.21.1 发布物，因此不写入 `neoforge.mods.toml`。相关定义、namespace 过滤和 `master_forge_ritual` 资源保留为休眠兼容代码；在该模组未来提供 1.21.1 版本前不会进入可用仪式列表。

## 资源与配方

17 份 Prefab 结构位于 `assets/prefab_goety_altars/structures/`。物品模型、纹理、建筑预览图和语言文件沿用原路径。两份 1.21.1 配方位于单数目录 `data/prefab_goety_altars/recipe/`：

```text
ritual_altar.json
all_ritual.json
```

扫描器优先将结构导出到可识别的源码资源目录；普通游戏环境回退到游戏目录下的 `prefab_goety_altars_exports/`。

## 构建

```bat
gradlew.bat clean compileJava
gradlew.bat build -x test
```

本次迁移按要求不运行 `runClient`、`runServer`、JUnit 或游戏内 smoke。游戏内放置、预览、扫描和旧世界加载由用户验证。
