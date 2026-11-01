# 永恒原版食物 (eternal_food_mod)

为《我的世界》1.20.1 **每一个原版食物**都添加一个「永恒」版本：
吃下去效果与原版一致，但**物品不消耗、没有冷却**，可以无限次食用。

> Minecraft 1.20.1 · Forge 47.x · Java 17 · MIT License

---

## 特性

- **68 种永恒食物**：苹果、面包、牛排、金苹果、紫颂果、炖菜、鱼……原版能吃的基本都有对应版本
- **不消耗、无冷却**：右键吃一口，食物还在，效果照常生效
- **继承原版行为**：食用动画 / 时长 / 音效 / 状态效果全部沿用原版物品
  （牛奶清效果、蜂蜜解毒、可疑炖菜读 NBT、紫颂果传送并触发图腾动画等）
- **完整合成链**：原版食物 + 下界之星 → 对应的永恒食物
- **永恒蛋糕是真正的方块**：能放置、右键吃不消失；可以驯服和投喂车万女仆
- **自带源码**：发布的 jar 里包含全部 `.java` 源码，方便审计与二次开发

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.0 及以上 |
| Java | 17 |

## 构建

```bash
git clone https://github.com/C2100425774/user_9k2h8ismc9exbn45-s-Eternal-Food
cd eternal_food_mod

# 1) 先准备车万女仆的编译期依赖（见 libs/README.md）
#    下载后放到 libs/touhoulittlemaid-1.5.3-forge+mc1.20.1.jar

# 2) 构建
./gradlew build          # Windows 用 gradlew.bat
```

产物：`build/libs/eternal_food_mod-<版本>.jar`

版本号规则：第二位是大更新，第三位是小改动 / 修 bug。
`./gradlew bumpVersion` 只会把第三位 +1。

### 为什么需要车万女仆的 jar 才能编译？

「永恒蛋糕能被女仆吃掉」这个功能是通过 TLM 的公开扩展 API 实现的，
编译期必须有它的类。但它是**软依赖**：运行时不装 TLM 也不会报错，
只是没有女仆相关功能（相关类只有在 TLM 扫描 `@LittleMaidExtension` 时才会被加载）。

## 车万女仆联动

| 功能 | 实现方式 |
|---|---|
| 用永恒蛋糕驯服女仆 | 物品标签 `touhou_little_maid:maid_tamed_item` + `forge:cakes` |
| 女仆会吃放在零食柜上的永恒蛋糕 | `ILittleMaid#registerMaidEdibleBlock` 注册自定义 `IMaidEdibleBlock` |
| 零食柜显示「有食物」状态 | 方块标签 `touhou_little_maid:snack_cabinet_full` |
| 手持永恒蛋糕时女仆卖萌 | Mixin 注入 `EntityMaid#getTemptationItem`（TLM 没有提供标签，只能改代码） |

## 目录结构

```
src/main/java/cn/autoforged/eternal_food_mod_1789477527/
├── block/      永恒蛋糕方块
├── item/       物品注册与各种自定义物品
├── event/      事件处理（发射器、铁砧合成、创造标签页等）
├── recipe/     自定义配方类型（满置域、烟花火箭、药箭等）
├── mixin/      Mixin（酿造台、玩家、车万女仆可选）
├── compat/     车万女仆扩展入口
├── datagen/    数据生成（模型 / 配方 / 战利品表）
└── ...
src/main/resources/
├── assets/     模型、贴图、语言文件
└── data/       配方、战利品表、标签、进度
```

## 许可证

[MIT](LICENSE)。你可以自由使用、修改、再分发，包括商用和闭源，
只需保留版权声明。如果基于本模组做了衍生作品，欢迎回链。

## 致谢

- [Touhou Little Maid](https://modrinth.com/mod/touhou-little-maid) —— 女仆联动基于其公开 API
- Minecraft Forge 与 MCP 社区