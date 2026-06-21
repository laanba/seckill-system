# Seckill System - 高并发秒杀系统

## 项目简介

这是一个基于 Spring Boot 2.7.x + Redis + RabbitMQ + MySQL 的高并发秒杀系统，采用微服务架构设计，实现了库存预扣减、分布式锁、限流防刷、异步下单等核心功能。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.18 | 核心框架 |
| MyBatis + Mapper | 2.2.2 / 4.2.5 | ORM框架 |
| MySQL | 8.0 | 主数据库 |
| Redis | - | 库存缓存、分布式锁、限流 |
| RabbitMQ | 4.3.0 | 异步消息队列 |
| JWT | 0.9.1 | 用户认证 |

## 项目结构

```
seckill-system/
├── seckill-common/          # 公共模块（工具类、常量、异常、配置）
├── seckill-api/             # API模块（实体、Mapper、Controller、DTO）
├── seckill-service/         # 服务模块（核心业务逻辑）
├── seckill-web/             # Web模块（启动类、配置文件）
├── sql/                     # SQL初始化脚本
└── pom.xml                  # 父POM
```

## 核心功能

### 1. 秒杀模块
- **库存预扣减**：基于 Redis Lua 脚本实现原子扣减，防止超卖
- **分布式锁**：使用 Redis SETNX 保证一人一单
- **限流防刷**：滑动窗口算法实现接口限流
- **异步下单**：用户请求先返回"排队中"，订单通过 RabbitMQ 异步创建

### 2. 商品模块
- 秒杀商品查询（正在进行/即将开始/已结束）
- 商品库存实时查询
- 活动状态自动管理

### 3. 订单模块
- 订单查询
- 订单支付/取消
- 超时自动取消（15分钟）

### 4. 用户模块
- JWT Token 认证
- 秒杀资格校验

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 4.3+

### 1. 初始化数据库

```bash
mysql -u root -p < sql/init.sql
```

### 2. 配置修改

编辑 `seckill-web/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/seckill_db
    username: your_username
    password: your_password
  
  redis:
    host: localhost
    port: 6379
  
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### 3. 启动服务

```bash
# 编译项目
mvn clean package -DskipTests

# 启动应用
cd seckill-web
java -jar target/seckill-web-1.0.0.jar
```

### 4. 测试账号

| 用户名 | 密码 | 说明 |
|--------|------|------|
| test001 | 123456 | 测试用户1 |
| test002 | 123456 | 测试用户2 |
| admin | 123456 | 管理员 |

## API 文档

### 用户接口

#### 登录
```http
POST /api/user/login
Content-Type: application/json

{
  "username": "test001",
  "password": "123456"
}
```

响应：
```json
{
  "code": 200,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "userId": 1,
    "username": "test001"
  }
}
```

### 商品接口

#### 获取秒杀商品列表
```http
GET /api/goods/list
```

#### 获取正在秒杀的商品
```http
GET /api/goods/ongoing
```

#### 获取商品详情
```http
GET /api/goods/{goodsId}
```

### 秒杀接口

#### 执行秒杀
```http
POST /api/seckill/execute
Authorization: Bearer {token}
Content-Type: application/json

{
  "goodsId": 1
}
```

响应：
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "orderNo": "SK20240101123456789012",
    "status": 0,
    "message": "Request is being processed, please wait"
  }
}
```

### 订单接口

#### 获取用户订单
```http
GET /api/order/list
Authorization: Bearer {token}
```

#### 支付订单
```http
POST /api/order/{orderNo}/pay
Authorization: Bearer {token}
```

#### 取消订单
```http
POST /api/order/{orderNo}/cancel
Authorization: Bearer {token}
```

## 核心流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                         秒杀请求流程                             │
└─────────────────────────────────────────────────────────────────┘

  用户请求
     │
     ▼
┌─────────────┐
│  Token校验  │ ─── 失败 ───▶ 返回未授权
└─────────────┘
     │ 成功
     ▼
┌─────────────┐
│  限流检查   │ ─── 超限 ───▶ 返回限流
└─────────────┘
     │ 通过
     ▼
┌─────────────┐
│ 商品状态检查 │ ─── 未开始/已结束 ───▶ 返回错误
└─────────────┘
     │ 有效
     ▼
┌─────────────┐
│ 一人一单检查 │ ─── 已购买 ───▶ 返回已购买
└─────────────┘
     │ 未购买
     ▼
┌─────────────┐
│ 获取分布式锁 │ ─── 失败 ───▶ 返回系统繁忙
└─────────────┘
     │ 成功
     ▼
┌─────────────┐
│ Redis原子   │ ─── 库存不足 ───▶ 释放锁,返回库存不足
│ 扣减库存    │
└─────────────┘
     │ 成功
     ▼
┌─────────────┐
│ 标记用户    │
│ 已购买      │
└─────────────┘
     │
     ▼
┌─────────────┐
│ 发送MQ消息  │
│ 异步创建订单│
└─────────────┘
     │
     ▼
┌─────────────┐
│ 释放分布式锁│
└─────────────┘
     │
     ▼
  返回成功
  (排队中)
```

## 秒杀流程时序图

```
┌────────┐    ┌────────┐    ┌────────┐    ┌────────┐    ┌────────┐
│  用户  │    │  API  │    │ Redis  │    │RabbitMQ│   │  DB   │
└───┬────┘    └───┬────┘    └───┬────┘    └───┬────┘    └───┬────┘
    │             │             │             │             │
    │ 1.秒杀请求  │             │             │             │
    │────────────>│             │             │             │
    │             │ 2.校验+限流  │             │             │
    │             │─────────────>             │             │
    │             │ 3.原子扣库存 │             │             │
    │             │─────────────>             │             │
    │             │ 4.扣减成功   │             │             │
    │             │<─────────────              │             │
    │             │ 5.发送MQ消息 │             │             │
    │             │──────────────────────────>│             │
    │             │ 6.返回排队中  │             │             │
    │<─────────────              │             │             │
    │             │             │             │ 7.消费消息  │
    │             │             │             │────────────>│
    │             │             │             │             │ 8.创建订单
    │             │             │             │             │────────────>│
    │             │             │             │             │ 9.更新库存
    │             │             │             │             │────────────>│
    │             │             │             │             │
    │             │             │             │             │
```

## 关键配置说明

### Redis Lua 脚本（库存扣减）

```lua
if redis.call('exists', KEYS[1]) == 1 then
    local stock = tonumber(redis.call('get', KEYS[1]));
    if stock > 0 then
        redis.call('decr', KEYS[1]);
        return 1;  -- 成功
    else
        return 0;  -- 库存不足
    end;
else
    return -1;     -- 库存未初始化
end;
```

### 限流滑动窗口

使用 Redis Sorted Set 实现滑动窗口限流，每个请求以时间戳为 score 存入集合，定期清理窗口外的请求。

## 常见问题

### 1. Redis 未启动
系统会降级处理，但秒杀功能将不可用。确保 Redis 正常启动后再使用秒杀功能。

### 2. RabbitMQ 未启动
订单创建将失败，需要确保 RabbitMQ 服务正常启动。

### 3. 库存超卖
通过 Redis Lua 脚本保证原子性扣减，理论上不会出现超卖。

### 4. 一人多单
通过 Redis Key 标记 + 分布式锁双重保证。

## 性能优化建议

1. **连接池优化**：调整 Druid/HikariCP 连接池参数
2. **Redis 集群**：使用 Redis Cluster 提高并发能力
3. **MQ 集群**：RabbitMQ 集群部署提高消息处理能力
4. **静态资源分离**：使用 CDN 加速静态资源
5. **数据库读写分离**：主从复制分散压力

## License

MIT License
