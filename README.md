
# NeverDie4

该项目为设计一个类吸血鬼幸存者的2D肉鸽游戏

```text
src/main/java/com/roguelike/
├── core/               // 游戏核心（生命周期、全局状态、事件）
├── entities/           // 所有游戏实体（玩家、敌人、道具等）(目前只考虑玩家道具)
├── map/                // 地图相关（生成、加载、障碍物管理）
├── ui/                 // 界面（HUD、菜单）(先只做菜单)
└── utils/              // 通用工具（随机数、坐标计算等）
```

## core/
#### GameApp.java:
- 核心生命周期接口：提供pauseGame()/resumeGame()（暂停/恢复游戏，封装FXGL菜单接口）。
- 游戏状态控制：gameOver()（触发游戏结束逻辑，联动Menus的showGameOver）。
- 模块协调中心：初始化游戏各模块（调用MapLoader加载地图、EntityFactory创建玩家）。
- 逻辑调度职责：负责全局状态管理、事件通信处理，协调实体、地图、UI模块交互。
- 可扩展接口：支持调用地图绘制、敌人生成等逻辑，多数场景作为接口调用中枢。

#### GameState.java:
- 玩家状态接口：getPlayerHP/setPlayerHP、damagePlayer/healPlayer（生命值管理）。
- 成长系统接口：addScore/getScore、nextLevel（分数与关卡进阶）。
- 数据存储核心：保存当前分数、玩家生命值、关卡数、已收集道具列表等关键数据。
- 实体归属管理：entities包中实体实例由GameApp生成，统一归属core包管理。
- 状态校验机制：提供合法性校验（如生命值不能为负）。

#### GameEvent.java:
- 事件通信接口：post(event)（发送事件，如玩家移动通知地图刷新）、listen(type, handler)（注册监听器，如监听地图加载完成）。
- 核心事件定义：包含PLAYER_MOVE、ENEMY_DEATH、MAP_LOADED等事件类型（枚举或类）。
- 事件转换逻辑：作为事件总线中转站，如Enemy.onDeath触发ENEMY_DEATH事件，进而转换为EXP_UP事件。
- 引擎封装：基于FXGL的EventBus，提供简化的事件发送/监听方法。


## entities/
#### EntityBase.java
- 基础属性管理：碰撞盒控制（setSize、getCollisionBox）、位置获取（getGamePosition）。
- 继承体系基础：扩展FXGL的Entity，封装位置、大小、碰撞箱等通用属性。
- 实体行为定义：统一规范动态实体（玩家、敌人、子弹等）的属性、行为及交互逻辑，是战斗与探索的核心载体。

#### Player.java(子类)
- 运动控制接口：move(dx, dy)（接受位移量实现移动）。
- 攻击行为接口：attack()（发射子弹，依赖AttackComponent）。
- 组件依赖：集成MoveComponent（移动控制）、AttackComponent（攻击逻辑）。

#### Enemy.java(子类)
- AI行为接口：onUpdate()（实现朝向玩家移动的简单逻辑）。
- 死亡处理接口：onDeath()（加分并移除实体，触发分数更新事件）。
- 组件依赖：包含HealthComponent（生命值管理），实现基础生存逻辑。

#### GameEntityFactory.java
- 实体创建接口：newPlayer()、newEnemy()（通过FXGL的@Spawns注解注册，支持外部调用spawn("player")生成实体）。
- 实例化管理：统一负责实体的创建与初始化，降低模块间耦合。

-- 建议扩展：抽象Bullet类，区分友方/敌方子弹，提供getDamage()、onHit()接口（当前Player.attack直接创建矩形，耦合度较高）。


## map/
#### TiledMap.java, Layer.java, Tileset.java
- 地图属性接口：getTilewidth/getTileheight（瓦片尺寸）、getLayers（图层列表）、getTilesets（瓦片集）。
- 辅助解析功能：为地图加载提供基础数据支持，解析Tiled编辑器导出的.tmx文件。

#### MapRenderer.java
- 初始化接口：init()（生成简单网格背景，供core包调用）。
- 更新接口：onUpdate()（支持视口跟随玩家移动，在core包中调度）。
- 地图加载接口：新增loadTiledMap(String path)（解析.tmx文件加载地图）。
- 碰撞判断支持：基于图层数据提供isPassable(int x, int y)（判断坐标是否可通行，供实体移动碰撞检测）。
- 资源与绘制：通过utils包的ResourceUtils加载瓦片资源，绘制地图背景和障碍物，每帧更新显示（如视口跟随）。

-- 未来扩展：支持动画瓦片（火焰、水流等），MapRenderer添加动画渲染逻辑。


## ui/
#### GameHUD.java
- 初始化接口：init()或mount()（添加HP条、分数显示到界面）。
- 状态更新接口：updateHP()（刷新HP条显示，绑定GameState的玩家HP）。
- 信息展示职责：显示生命值条、分数文本、基于MapData的简化小地图。
- 事件响应：监听PLAYER_HURT事件更新血条，监听SCORE_CHANGED事件更新分数。

#### Menus.java
- 菜单展示接口：showStartMenu()、showPauseMenu()、showGameOver()（显示对应菜单并绑定交互回调）。
- 菜单管理接口：hideAll()（关闭所有菜单，供core包调用恢复游戏）。
- 交互逻辑：包含开始菜单、暂停菜单，按钮回调关联core包初始化方法（如“开始游戏”触发游戏初始化）。

-- 提示功能扩展：新增showTooltip(String text, Duration duration)（显示临时提示，如“获得道具”）。
-- 进度展示扩展：为GameHUD添加updateLevel()、updateItems()接口（显示当前等级、收集的道具）。


## utils/
多数为开放接口，提供通用功能支持

#### RandomUtils.java
- 随机数生成接口：nextInt(min, max)（范围内整数生成，用于地图随机、敌人刷新）、nextBool(chance)（按概率返回布尔值，如道具掉落概率）。

#### MathUtils.java
- 坐标转换：gridToScreen(int x, int y)（地图网格坐标转屏幕像素坐标）。
- 碰撞辅助：isPointInRect(Point, Rectangle)（判断点是否在矩形内，支持碰撞检测）。

-- 向量工具扩展：新增VectorUtils类，提供distance(p1, p2)（两点距离计算）、normalize(vec)（向量归一化）等接口（供实体移动、攻击方向计算使用）。
-- 资源工具扩展：封装ResourceUtils，提供loadTexture(path)（图片加载）、loadSound(path)（音效加载）接口，统一处理资源缓存与异常。
```