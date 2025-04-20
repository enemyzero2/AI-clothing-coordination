# AI衣搭 - 后端API服务

## 项目概述
AI衣搭应用的后端API服务，提供用户认证、衣物管理、搭配推荐等RESTful接口。

## 技术栈
- Spring Boot 2.7.5
- Spring Security
- Spring Data JPA
- MySQL 8.0
- JWT认证
- Maven

## 开发环境配置
1. 安装Java JDK 11或更高版本
2. 安装Maven
3. 安装MySQL 8.0
4. 创建数据库和用户：
   ```sql
   CREATE DATABASE ai_outfit_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'aioutfit'@'localhost' IDENTIFIED BY 'your-password';
   GRANT ALL PRIVILEGES ON ai_outfit_app.* TO 'aioutfit'@'localhost';
   FLUSH PRIVILEGES;
   ```

## 构建与运行
1. 修改数据库配置：在`src/main/resources/application.properties`中更新数据库连接信息
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/ai_outfit_app?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
   spring.datasource.username=aioutfit
   spring.datasource.password=your-password
   ```

2. 构建项目：
   ```bash
   mvn clean package
   ```

3. 运行项目：
   ```bash
   java -jar target/aioutfitapp-0.0.1-SNAPSHOT.jar
   ```

4. 也可直接在开发环境中运行：
   ```bash
   mvn spring-boot:run
   ```

## API文档

### 认证API

#### 用户注册
- URL: `/api/auth/register`
- 方法: POST
- 请求体:
  ```json
  {
    "username": "your-username",
    "email": "your-email@example.com",
    "password": "your-password"
  }
  ```
- 响应:
  ```json
  {
    "success": true,
    "message": "注册成功",
    "userId": "user-id",
    "username": "your-username",
    "email": "your-email@example.com"
  }
  ```

#### 用户登录
- URL: `/api/auth/login`
- 方法: POST
- 请求体:
  ```json
  {
    "username": "your-username",
    "password": "your-password"
  }
  ```
- 响应:
  ```json
  {
    "success": true,
    "message": "登录成功",
    "token": "jwt-token",
    "tokenType": "Bearer",
    "id": "user-id",
    "username": "your-username",
    "email": "your-email@example.com",
    "roles": ["ROLE_USER"]
  }
  ```

## 安全配置
- 所有API都需要进行认证，除了`/api/auth/**`和`/api/test/**`
- 使用JWT令牌进行认证，令牌有效期为24小时
- 需在请求头中添加令牌:
  ```
  Authorization: Bearer your-jwt-token
  ```

## 数据库初始化
- 应用启动时会自动执行`schema.sql`和`data.sql`创建所需表和初始数据
- `roles`表会初始化两个角色：`ROLE_USER`和`ROLE_ADMIN`

## 项目结构
```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── aioutfitapp/
│   │   │               ├── controller/      - 控制器
│   │   │               ├── dto/             - 数据传输对象
│   │   │               │   ├── request/     - 请求DTO
│   │   │               │   └── response/    - 响应DTO
│   │   │               ├── model/           - 实体模型
│   │   │               ├── repository/      - 数据访问层
│   │   │               ├── security/        - 安全配置
│   │   │               │   ├── jwt/         - JWT认证
│   │   │               │   └── service/     - 安全服务
│   │   │               ├── service/         - 业务逻辑层
│   │   │               │   └── impl/        - 服务实现
│   │   │               └── AiOutfitAppApplication.java
│   │   └── resources/
│   │       ├── application.properties       - 应用配置
│   │       ├── data.sql                     - 数据初始化脚本
│   │       └── schema.sql                   - 表结构初始化脚本
│   └── test/                                - 测试代码
├── pom.xml                                  - Maven配置
└── README.md                                - 文档说明
```

## 常见问题
1. **Q: 如何更改JWT密钥?**
   A: 在`application.properties`中更新`jwt.secret`参数

2. **Q: 如何连接到远程MySQL服务器?**
   A: 更新`spring.datasource.url`为远程服务器地址，确保正确配置用户名和密码

3. **Q: 如何更改API的上下文路径?**
   A: 在`application.properties`中修改`server.servlet.context-path`值

4. **Q: 如何添加管理员用户?**
   A: 先注册一个普通用户，然后在数据库中为该用户添加`ROLE_ADMIN`角色 