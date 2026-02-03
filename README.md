# HuShen Web Game

一般向二游狐神物语展示 Demo，前端为静态页面，后端为 Spring Boot + MySQL。

## 运行环境

- Windows 10/11（其他系统也可）
- JDK 17
- Maven 3.8+
- MySQL 5.7+（建议 8.0，5.7 有兼容警告但可用）

## 项目结构

- `fronted/`：前端静态资源（`index.html`/`styles.css`/`app.js`）
- `backed/`：Spring Boot 后端
- `images/`：可选的外部图片目录（也支持放在 `fronted/images/`）

## 数据库准备

1. 创建数据库（示例名 `hushen`）：

   ```sql
   CREATE DATABASE hushen DEFAULT CHARSET utf8mb4;
   ```

2. 修改后端配置：

   `backed/src/main/resources/application.properties`

   - `spring.datasource.url`：数据库地址
   - `spring.datasource.username`/`password`
   - `jwt.secret`：JWT 密钥（建议 32 位以上随机串）
   - `jwt.expirationMinutes`：JWT 过期时间（分钟）

3. 建表与基础数据（推荐使用初始化脚本）：

   ```bash
   mysql -u root -p < db/init.sql
   ```

   这个脚本会：
   - 创建所有表
   - 插入基础角色/皮肤/技能/卡池
   - 设定默认主页角色和皮肤

## 启动后端

在 `backed/` 目录执行：

```bash
mvn spring-boot:run
```

默认端口：`http://localhost:3000`

后端会自动：
- 扫描图片文件并同步到数据库（角色/皮肤/技能/卡池）
- 自动创建/更新表结构（`spring.jpa.hibernate.ddl-auto=update`）

## 访问前端

后端会将 `fronted/` 作为静态资源目录：

```
http://localhost:3000/
```

## 登录说明（JWT）

- 登录/注册成功会返回 `token`
- 前端请求会自动携带 `Authorization: Bearer <token>`
- 30 分钟无操作会过期，操作会自动续期

## 图片命名规则（自动同步）

你只需要把图片放入对应文件夹，后端会自动识别并入库。

### 1) 角色立绘

目录：`images/character/` 或 `fronted/images/character/`

```
character1.png
character2.jpg
character3.jpg
```

### 2) 角色皮肤

目录：`images/character_skins/` 或 `fronted/images/character_skins/`

```
character1_skin2.png
character2_skin3.jpg
```

### 3) 技能图片

目录：`images/skills/` 或 `fronted/images/skills/`

```
character1_bigSkill.png
character1_skill1.png
character1_skill2.png
```

### 4) 卡池背景

目录：`images/cardPool/` 或 `fronted/images/cardPool/`

```
cardPoolBackground1.png
cardPoolBackground2.png
cardPoolBackground3.png
```

## 常见问题

- **看不到新图片**：重启后端并访问一次 `/api/characters` 或打开页面刷新即可
- **登录过期提示**：重新登录，前端会自动续期
- **数据库没有表**：确认后端已启动并成功连接数据库

## 备注

- `fronted/images/` 与 `images/` 都可以放资源
- 建议将 `jwt.secret` 改为自己的随机值
- 持续更新 GitHub，如有问题/建议，或愿意提供自己的 OC 进池子，请发送邮件至 `hushenwuyudemo@163.com`
