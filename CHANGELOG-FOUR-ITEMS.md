# 四物品修正文件清单

本次只修正类型映射、资源及相应测试。没有新增 Java 文件。ModItems、GoetyAltarItem、GoetyAltarScreen、配置、网络协议/包、材质和原生建造代码均未修改。

## 新增

- `src/main/resources/assets/prefab_goety_altars/models/item/all_ritual.json`
- `src/main/resources/assets/prefab_goety_altars/models/item/master_forge_ritual.json`
- `src/main/resources/assets/prefab_goety_altars/models/item/revelation_all_ritual.json`
- `src/main/resources/assets/prefab_goety_altars/models/item/ritual_altar.json`

## 修改

- `src/main/java/com/prefabgoetyaltars/client/ModItemModels.java`
- `src/main/java/com/prefabgoetyaltars/compat/revelation/RevelationAltarRegistration.java`
- `src/main/java/com/prefabgoetyaltars/structure/AltarPrefabType.java`
- `src/main/resources/META-INF/mods.toml`
- `src/main/resources/assets/prefab_goety_altars/lang/en_us.json`
- `src/main/resources/assets/prefab_goety_altars/lang/zh_cn.json`
- `src/smoke/java/com/prefabgoetyaltars/smoke/AltarSmokeChecks.java`
- `src/smoke/java/com/prefabgoetyaltars/smoke/MaterialSmokeChecks.java`
- `src/smoke/java/com/prefabgoetyaltars/smoke/SmokeChecks.java`
- `src/test/java/com/prefabgoetyaltars/network/BuildAltarPacketTest.java`
- `src/test/java/com/prefabgoetyaltars/structure/RitualDefinitionTest.java`

## 移除重复或旧资源

- `src/generated/resources/assets/prefab_goety_altars/models/item/all_ritual_altar.json`
- `src/generated/resources/assets/prefab_goety_altars/models/item/revelation_all_ritual_altar.json`
- `src/generated/resources/assets/prefab_goety_altars/models/item/ritual_altar.json`

## 文档

更新 README.md、VERIFICATION.md；新增本清单。原三物品变更说明改为指向当前文档，修改前历史内容保存在本地快照中。未删除历史日志、备份或用户纹理。

## 派生输出

刷新了现有 bin 编译/资源副本，并移除其中两个过期模型，避免 IDE 输出保留旧注册/语言引用。这些是派生文件，不是新增源码。Gradle 产物由最终 clean build 重新生成。
