# Changelog

## 1.0.0

- Forge 1.20.1 framework for Prefab and Goety ritual altar items.
- Shared definition, item, screen, configuration, structure and server packet.
- No altar definitions, structures, textures or recipes are included yet.

- 新增开发用祭坛结构扫描器：常驻注册、开发权限限制、可保存 GUI 草稿、独立范围线框和 Prefab 原生导出。
- 网络协议升为 2；保留建造消息编号 0，新增扫描器消息 1～3。
- 新增范围、配置、文件安全测试和隔离的客户端集成测试；原祭坛定义与依赖版本保持不变。

## 扫描器可用性修复

- 移除功能路径中的 production、OP 和创造模式限制；production 仅控制创造标签页显示。
- 客户端与服务端距离上限统一为 16 格；失败原因分别本地化并记录日志。
- 无源码项目时回退到实际游戏目录的 prefab_goety_altars_exports，成功提示显示绝对路径。
- 保持 Prefab 原生导出、禁止覆盖、网络协议 2 和现有依赖范围。

## 六种本体祭坛与可选启示录兼容

- 注册活力、锻造、霜冻、大地、死灵、增生六种共享祭坛物品，按原字节打包现有结构。
- 新增六份生成模型和中英文名称，不生成贴图、配方或启示录占位物品。
- 增加可空 requiredModId 与安全的可选启示录入口，使用和建造前验证依赖，注册表不按依赖动态变化。
- 增加资源路径日志、固定定义测试及隔离的四方向、双手 GUI/网络建造测试。
