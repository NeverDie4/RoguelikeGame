最小可行版本
src/main/java/com/roguelike/
├── core/               // 游戏核心（生命周期、全局状态、事件）
├── entities/           // 所有游戏实体（玩家、敌人、道具等）(目前只考虑玩家道具)
├── map/                // 地图相关（生成、加载、障碍物管理）(+7做一张简单地图)
├── ui/                 // 界面（HUD、菜单）(先只做菜单)
└── utils/              // 通用工具（随机数、坐标计算等）


依据项目结构而写，“开放接口：”后为需要公共使用的函数或类，“--”开头为未完成的功能

core/
    GameApp.java:
        游戏核心逻辑的调度中心，负责初始化游戏、管理全局状态、处理事件通信、协调各模块交互。
        初始化其他模块（如调用 MapLoader 加载地图、EntityFactory 创建玩家）。
        开放接口：pauseGame()/resumeGame()：暂停 / 恢复游戏（当前依赖 FXGL 自带菜单，需封装为核心接口）。
                gameOver()：触发游戏结束逻辑（目前 Menus 有showGameOver，需与核心状态联动）。
    GameState.java:
        存储游戏关键数据：当前分数、玩家生命值、关卡数、已收集道具列表等。
        提供状态修改的合法性校验（如生命值不能为负）。
        开放接口：getPlayerHP/setPlayerHP、damagePlayer/healPlayer（生命值管理）。
                addScore/getScore、nextLevel（成长系统基础）。
                addItem/getCollectedItems（道具系统基础）。
    GameEvent.java:
        定义核心事件类型（枚举或类）：PLAYER_MOVE、ENEMY_DEATH、MAP_LOADED 等。
        封装 FXGL 的 EventBus，提供简化的事件发送 / 监听方法。
        开放接口：GameEvent.post(event)	发送事件（如玩家移动后通知地图刷新）
                GameEvent.listen(type, handler)	注册事件监听器（如监听地图加载完成）
entities/
    EntityBase.java
        继承 FXGL 的 Entity，封装基础属性（位置、大小、碰撞箱）。
        定义游戏中所有动态实体（玩家、敌人、子弹等）的属性、行为和交互逻辑，是战斗和探索的核心载体。
        开放接口：碰撞盒管理（setSize、getCollisionBox）、位置获取（getGamePosition）。
    Player.java(子类)
        玩家实体，包含移动（move(dx, dy)）、攻击（attack()）方法，依赖 MoveComponent、AttackComponent。
        开放接口：运动接口：move（接受位移量）。 攻击接口：attack（发射子弹）。
    Enemy.java(子类)
        敌人实体，包含简单 AI 逻辑（updateAI()，如向玩家移动），依赖 HealthComponent。
        开放接口：AI 行为接口：onUpdate（朝向玩家移动）。 死亡逻辑接口：onDeath（加分并移除实体）。
    GameEntityFactory.java
        开放接口：实体创建接口：newPlayer/newEnemy（通过 FXGL 的@Spawns注解注册，支持从外部调用spawn("player")生成实体）。
                为Player/Enemy添加属性设置器（如setSpeed、setDamage）
    --建议将子弹作为实体类，并且将其抽象，便于玩家与敌人弹幕的设计
    --抽象Bullet类，区分友方 / 敌方子弹，提供getDamage、onHit接口（当前Player.attack直接创建矩形，耦合度高）。
map/
    TiledMap.java,Layer.java,Tileset.java
        开放接口：getTilewidth/getTileheight（瓦片尺寸）、getLayers（图层列表）、getTilesets（瓦片集）。
    MapRenderer.java
        使用Tiled创建地图，加载瓦片资源（通过 utils 包的 ResourceUtils），绘制地图背景和障碍物。
        每帧更新地图显示（如跟随玩家移动的视口）。
        开放接口：提供地图元数据访问接口：初始化接口：init（生成简单网格背景）。 更新接口：onUpdate（支持视口跟随）。
                新增loadTiledMap(String path)接口（解析 Tiled 地图编辑器的.tmx文件），替代当前硬编码的网格背景。
                基于图层数据提供isPassable(int x, int y)（判断坐标是否可通行），支持实体移动时的碰撞判断（当前无地图碰撞）。
    --未来扩展Tileset支持动画瓦片（如火焰、水流），MapRenderer添加动画渲染逻辑。
ui/
    GameHUD.java
        显示玩家状态：生命值条（绑定 GameState 的玩家 HP）、分数文本、小地图（基于 MapData 网格绘制简化版）。
        监听 PLAYER_HURT 事件更新血条，监听 SCORE_CHANGED 事件更新分数。
        开放接口：初始化接口：mount（添加 HP 条、分数到界面）。 状态更新接口：updateHP（刷新 HP 条显示）。
    Menus.java
        包含开始菜单（showStartMenu()）、暂停菜单（showPauseMenu()），提供按钮回调（如 “开始游戏” 调用 core 包的初始化方法）。
        开放接口：菜单展示接口：showStartMenu/showPauseMenu/showGameOver（显示对应菜单并绑定回调）。 菜单隐藏接口：hideAll（关闭所有菜单）。
    --提示信息接口： 新增showTooltip(String text, Duration duration)（显示临时提示，如 “获得道具”）。
    --进度展示接口： 为GameHUD添加updateLevel/updateItems接口（显示当前等级、收集的道具）。
utils/
    RandomUtils.java
        开放接口：封装随机数生成：nextInt(min, max)（生成范围内整数，用于地图随机、敌人刷新）、nextBool(chance)（按概率返回布尔值，如道具掉落概率）。
    MathUtils.java
        开放接口：坐标转换：gridToScreen(int x, int y)（将地图网格坐标转为屏幕像素坐标）。
                碰撞辅助：isPointInRect(Point, Rectangle)（判断点是否在矩形内）。
    --向量运算工具： 新增VectorUtils类，提供distance(p1, p2)（两点距离）、normalize(vec)（向量归一化）等接口（实体移动、攻击方向计算需频繁使用）。
    --封装ResourceUtils，提供loadTexture(path)（加载图片）、loadSound(path)（加载音效）接口，统一处理资源缓存与异常。
