# Prefab - Goety Ritual Altars

Minecraft 1.20.1 / Forge 47.4.20 / Java 17，modid `prefab_goety_altars`。

## 两个祭坛物品

| Registry ID | 行为 |
|---|---|
| `ritual_altar` | 选择具体仪式、祭坛材质、基座材质和方向 |
| `all_ritual` | 固定加载 `all_ritual.gz`，只选择两种材质和方向 |

两个物品均通过 `DeferredRegister` 注册并继续使用 Prefab `StructureItem`、原生 Preview 和本模组 `BuildAltarPacket`。网络协议为 4，旧四物品客户端不能连接。成功仅扣实际交互手一件，失败不扣。

`ritual_altar` 的固定顺序为 14 个 Goety 本体仪式，然后是条件选项：

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
master_forge_ritual   （需要 goety_revelation）
culinary_ritual        （需要 goetydelight）
```

GUI 只使用 `RitualDefinition.available()`，因此未安装的条件仪式不会出现。服务端重新验证定义与依赖，伪造请求不能绕过。大师锻造和烹饪仪式只是选项，不注册独立物品或配方。

## 可选模组

Goety Revelation 仍为可选依赖。Goety's Delight 的真实 modid 已从 1.4.6 jar 元数据确认是 `goetydelight`，声明范围为 `[1.4.6,)`。代码不引用两个附属模组的 Java 类。

`all_ritual.gz` 是唯一全仪式结构，并始终可用。加载出的独立结构实例会过滤缺失附属模组的内容：

- 无 Goety's Delight：删除 `shade_stove`、`cursed_ingot_pot` 及相同相对位置的两个 BlockEntity。
- 无 Goety Revelation：删除 `runestone_engraved_table`。
- 两者都有：保留全部内容。

过滤递归处理 `BuildBlock.getSubBlock()`，只允许上述两个 namespace 因对应模组缺失而被删除。未知方块、已安装模组仍缺失的注册项、错误 BlockEntity 或实体仍会拒绝加载。源 `.gz` 和 NBT 不会被改写。过滤在材质转换前完成，因此 11×11 祭坛/基座材质选择保持原行为。

## 资源与配方

祭坛物品模型和配方各只有：

```text
ritual_altar.json
all_ritual.json
```

`master_forge_ritual.gz/.png`、`culinary_ritual.gz/.png` 保留为轮换选项；`all_ritual.gz/.png` 为固定全仪式资源。祭坛扫描器及其资源保持不变。

## 构建

```bat
gradlew.bat clean build -x test
```

本轮按用户要求不运行 JUnit、smoke、客户端或服务端测试。游戏内兼容组合由用户自行验证。
