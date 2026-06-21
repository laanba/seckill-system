# 秒杀系统 技术文档

## 1. 项目概述

**项目名称**: seckill-system (高并发秒杀系统)  
**版本**: 1.0.0-SNAPSHOT  
**架构**: Spring Boot 2.7.18 + MyBatis + Redis + RabbitMQ  
**Java 版本**: 1.8  

本项目是一个典型的电商秒杀后端系统，核心目标是解决高并发场景下商品库存扣减的原子性、数据一致性、接口限流以及削峰填谷等问题。

---

## 2. 技术栈

| 组件 | 技术选型 | 版本 | 用途 |
|------|---------|------|------|
| Web 框架 | Spring Boot Starter Web | 2.7.18 | RESTful API |
| ORM | MyBatis + TK Mapper | 2.3.1 / 2.1.5 | 数据库访问、通用 CRUD |
| 数据库 | MySQL | 8.0.33 | 持久化存储 |
| 缓存 | Spring Data Redis | (集成) | 库存预扣、分布式锁、用户下单标记 |
| 消息队列 | Spring AMQP (RabbitMQ) | (集成) | 异步下单、削峰填谷 |
| 认证 | JJWT | 0.11.5 | JWT Token 签发与校验 |
| 参数校验 | Spring Boot Validation | (集成) | 请求参数校验 |
| 工具 | Lombok | 1.18.28 | 代码简化 |

---

## 3. 系统架构概览

```
┌──────────┐     ┌──────────────┐     ┌─────────┐     ┌──────────┐
│  Client  │────▶│  Controller  │────▶│ Service │────▶│  Mapper  │──▶ MySQL
└──────────┘     └──────────────┘     └─────────┘     └──────────┘
                      │                    │
                      │              ┌─────┴─────┐
                      │              ▼           ▼
                      │         ┌───────┐  ┌──────────┐
                      │         │ Redis │  │ RabbitMQ │
                      │         └───────┘  └──────────┘
                      │                           │
                      │                     ┌─────▼──────┐
                      │                     │  Consumer  │
                      │                     └────────────┘
```

**核心秒杀流程**:

1. 客户端携带 JWT Token 调用秒杀 API
2. Controller 层做 Token 校验
3. Service 层依次执行：**限流检查 → 商品校验 → 一人一单检查 → 生成订单号 → 分布式锁 → Redis 原子扣库存 → 标记用户已下单 → 发送 RabbitMQ 消息**
4. 立刻返回"排队中"的响应给客户端（异步削峰）
5. MQ 消费者异步消费消息，完成数据库级别的订单创建

---

## 4. Controller API 文档

系统共 **4 个 Controller**，所有接口统一返回 `Result<T>` 格式：

```json
{
  "code": 200,
  "message": "Success",
  "data": { ... }
}
```

### 4.1 UserController — 用户模块

**基础路径**: `/api/user`

#### 4.1.1 用户登录

```
POST /api/user/login
```

**描述**: 用户登录，成功后返回 JWT Token。后续所有需要鉴权的接口都需在 `Authorization` 请求头中携带此 Token。

**请求体**:
```json
{
  "username": "string (必填)",
  "password": "string (必填)"
}
```

**成功响应 (200)**:
```json
{
  "code": 200,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "test_user"
  }
}
```

---

#### 4.1.2 获取当前用户信息

```
GET /api/user/info
```

**描述**: 通过 Token 获取当前登录用户的基本信息。

**请求头**:
| 名称 | 说明 |
|------|------|
| Authorization | JWT Token (必填) |

**成功响应 (200)**:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "username": "test_user",
    "phone": "13800138000",
    "email": "test@example.com",
    "status": 1,
    "createTime": "2026-01-01 10:00:00"
  }
}
```

**错误响应 (401)**: Token 无效或未提供时返回 `"Please login first"`。

---

#### 4.1.3 验证 Token

```
GET /api/user/validate
```

**描述**: 校验 Token 是否有效，返回对应的用户 ID。可用于前端初始化时快速判断登录状态。

**请求头**:
| 名称 | 说明 |
|------|------|
| Authorization | JWT Token (必填) |

**成功响应 (200)**:
```json
{
  "code": 200,
  "message": "Success",
  "data": 1
}
```

**错误响应 (401)**: Token 无效时返回 `"Invalid token"`。

---

### 4.2 GoodsController — 商品模块

**基础路径**: `/api/goods`

#### 4.2.1 获取所有商品列表

```
GET /api/goods/list
```

**描述**: 获取系统中全部秒杀商品，不区分状态。

**成功响应 (200)**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "goodsName": "iPhone 15 Pro",
      "goodsDesc": "限时秒杀，手慢无",
      "goodsPicture": "https://...",
      "originalPrice": 9999.00,
      "seckillPrice": 6999.00,
      "stock": 80,
      "totalStock": 100,
      "startTime": "2026-06-20 10:00:00",
      "endTime": "2026-06-20 12:00:00",
      "status": 2,
      "statusText": "进行中",
      "remainingSeconds": 0,
      "discountRate": 30
    }
  ]
}
```

---

#### 4.2.2 获取进行中的秒杀商品

```
GET /api/goods/ongoing
```

**描述**: 只返回当前正在秒杀中的商品（status = 2 且在有效时间范围内）。

---

#### 4.2.3 获取即将开始的秒杀商品

```
GET /api/goods/coming-soon
```

**描述**: 只返回即将开始秒杀的商品（status = 1，活动未开始）。`remainingSeconds` 字段表示距离开始的剩余秒数。

---

#### 4.2.4 获取商品详情

```
GET /api/goods/{goodsId}
```

**描述**: 根据商品 ID 获取单个商品的详细信息。

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| goodsId | Long | 商品 ID |

**错误响应 (404)**: 商品不存在时返回 `"Goods not found"`。

---

#### 4.2.5 获取商品库存

```
GET /api/goods/{goodsId}/stock
```

**描述**: 获取指定商品的当前剩余库存，用于前端实时库存展示和倒计时轮询。

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| goodsId | Long | 商品 ID |

**成功响应 (200)**:
```json
{
  "code": 200,
  "data": 80
}
```

---

### 4.3 SeckillController — 秒杀模块 (核心)

**基础路径**: `/api/seckill`

#### 4.3.1 执行秒杀

```
POST /api/seckill/execute
```

**描述**: **核心秒杀入口**。用户点击"立即抢购"时调用此接口。内部执行流程：

> Token 验证 → 频率限流 → 商品状态校验 → 一人一单检查 → 生成订单号(SK 前缀) → 获取分布式锁 → Redis 原子扣库存 → 标记用户已下单 → 发送 MQ 消息 → 立即返回"排队中"

**请求头**:
| 名称 | 说明 |
|------|------|
| Authorization | JWT Token (必填) |

**请求体**:
```json
{
  "goodsId": 1
}
```

**成功响应 (200) — 排队中**:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "orderId": null,
    "orderNo": "SK2026062015364278901",
    "status": 0,
    "message": "Request is being processed, please wait"
  }
}
```

**成功响应 (200) — 秒杀成功**:
```json
{
  "code": 200,
  "data": {
    "orderId": 12345,
    "orderNo": "SK2026062015364278901",
    "status": 1,
    "message": "Seckill successful, please complete payment"
  }
}
```

**常见错误码**:

| HTTP Code | message | 说明 |
|-----------|---------|------|
| 401 | Please login first | Token 未提供或已过期 |
| 400 | Invalid request | goodsId 为空或无效 |
| 400 | Stock not enough | 库存不足 |
| 400 | You have already purchased this item | 重复购买（一人一单） |
| 400 | Seckill has not started yet | 秒杀尚未开始 |
| 400 | Seckill has ended | 秒杀已结束 |
| 429 | Too many requests, please try again later | 触发限流 |
| 500 | System error, please try again later | 系统内部错误 |

**响应状态 (status) 说明**:

| status | 含义 |
|--------|------|
| 0 | 排队中 — 请求已接受，正在异步处理 |
| 1 | 秒杀成功 — 订单已生成，请尽快付款 |
| 2 | 秒杀失败 — 库存不足、重复购买等原因 |

---

#### 4.3.2 查询秒杀结果

```
GET /api/seckill/result/{orderNo}
```

**描述**: 客户端在秒杀提交后，可轮询此接口查询订单创建结果。当前版本返回排队状态，后续可扩展为查询数据库中的订单实际状态。

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| orderNo | String | 秒杀提交时返回的订单号（SK 开头） |

**说明**: 此接口为预留接口，生产环境下应在此查询数据库订单表，返回真实的创建/支付/取消状态。

---

### 4.4 OrderController — 订单模块

**基础路径**: `/api/order`

> 所有接口均需携带 `Authorization` 请求头。

#### 4.4.1 获取用户订单列表

```
GET /api/order/list
```

**描述**: 获取当前登录用户的所有秒杀订单。

**成功响应 (200)**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 12345,
      "orderNo": "SK2026062015364278901",
      "userId": 1,
      "goodsId": 1,
      "goodsName": "iPhone 15 Pro",
      "goodsPrice": 6999.00,
      "quantity": 1,
      "totalAmount": 6999.00,
      "status": 0,
      "statusText": "待支付",
      "payTime": null,
      "cancelTime": null,
      "expireTime": "2026-06-20 10:15:00",
      "createTime": "2026-06-20 10:00:00",
      "remainingSeconds": 842
    }
  ]
}
```

---

#### 4.4.2 获取订单详情

```
GET /api/order/{orderNo}
```

**描述**: 根据订单号获取单笔订单的详细信息。**会校验订单归属**，非本人订单返回 404。

---

#### 4.4.3 支付订单

```
POST /api/order/{orderNo}/pay
```

**描述**: 对指定订单号发起支付。会校验订单归属和订单状态。

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| orderNo | String | 订单号 |

**成功响应 (200)**:
```json
{
  "code": 200,
  "message": "Payment successful",
  "data": {
    "orderNo": "SK2026062015364278901",
    "status": 1,
    "statusText": "已支付"
  }
}
```

---

#### 4.4.4 取消订单

```
POST /api/order/{orderNo}/cancel
```

**描述**: 取消指定订单。会校验订单归属和是否可取消（仅"待支付"和"创建中"状态可取消）。取消后系统会自动回滚 Redis 库存和用户下单标记。

**成功响应 (200)**:
```json
{
  "code": 200,
  "message": "Order cancelled",
  "data": { ... }
}
```

---

## 5. 订单状态流转

```
        创建中(4)
           │
           ▼
       待支付(0) ──超时──▶ 已超时(3)
           │
      ┌────┴────┐
      ▼         ▼
  已支付(1)   已取消(2)
```

| 状态码 | 状态名 | 说明 |
|--------|--------|------|
| 0 | 待支付 | 订单已创建，等待用户付款，有超时限制 |
| 1 | 已支付 | 支付完成 |
| 2 | 已取消 | 用户主动取消 |
| 3 | 已超时 | 超过支付时限自动过期 |
| 4 | 创建中 | 异步下单中间状态 |

---

## 6. 商品状态流转

```
未发布(0) → 即将开始(1) → 进行中(2) → 已结束(3)
```

状态由 `SeckillScheduler` 定时任务每 30 秒根据当前时间与 `startTime`/`endTime` 的关系自动更新。

---

## 7. Redis 键设计

| Key 前缀 | 格式 | 用途 | 类型 |
|----------|------|------|------|
| `seckill:stock:` | `seckill:stock:{goodsId}` | 商品库存缓存 | String (int) |
| `seckill:lock:` | `seckill:lock:{userId}_{goodsId}` | 分布式锁 | String (SET NX) |
| `seckill:user:order:` | `seckill:user:order:{userId}:{goodsId}` | 用户下单标记（一人一单） | String |
| `seckill:ratelimit:` | `seckill:ratelimit:{userId}` | 用户访问频率限流 | String |
| `seckill:goods:status:` | `seckill:goods:status:{goodsId}` | 商品状态缓存 | String |

---

## 8. RabbitMQ 消息结构

**Exchange**: `seckill_exchange` (Topic 类型)  
**Queue**: `seckill_queue` (持久化)  
**Routing Key**: `seckill_order`

**消息体** (`Message`)：
```json
{
  "userId": 1,
  "goodsId": 1,
  "orderNo": "SK2026062015364278901",
  "timestamp": 1718872602000
}
```

消费者 (`SeckillMessageConsumer`) 消费消息后，执行数据库层面的库存扣减和订单写入操作。

---

## 9. 安全与并发控制

| 机制 | 实现层 | 说明 |
|------|--------|------|
| JWT 认证 | UserService + JwtUtil | 所有写操作和订单查询需 Token |
| 频率限流 | RateLimitService (Redis) | 防止同一用户高频刷接口 |
| 一人一单 | UserOrderCheckService (Redis) | 同一用户同一商品仅可秒杀一次 |
| 分布式锁 | DistributedLockService (Redis SET NX) | 保证库存扣减和下单标记的原子性 |
| 原子库存扣减 | RedisStockService (DECR) | Redis 单线程模型保证原子性 |
| 异常回滚 | SeckillService catch 块 | SeckillException 时回滚库存和下单标记 |
| 订单归属校验 | OrderController | 查询/支付/取消时校验 userId |

---

## 10. 项目目录结构

```
seckill-system/
├── pom.xml
└── seckill-api/
    └── src/main/java/com/seckill/api/
        ├── SeckillApplication.java          // 启动类
        ├── Component.java                     // Spring 组件扫描标记
        ├── UserRegisteredEvent.java           // 用户注册事件
        ├── config/
        │   ├── RabbitMQConfiguration.java     // RabbitMQ 交换机/队列/绑定
        │   ├── SeckillScheduler.java          // 定时任务(商品状态更新)
        │   └── StartupRunner.java             // 启动后初始化(Redis 库存预热)
        ├── constant/
        │   └── SeckillConstant.java           // 系统常量定义
        ├── controller/
        │   ├── GoodsController.java           // 商品 Controller
        │   ├── OrderController.java           // 订单 Controller
        │   ├── SeckillController.java         // 秒杀 Controller (核心)
        │   └── UserController.java            // 用户 Controller
        ├── dto/
        │   ├── GoodsDTO.java                  // 商品详情 DTO
        │   ├── LoginRequest.java              // 登录请求
        │   ├── LoginResponse.java             // 登录响应
        │   ├── Message.java                   // 秒杀消息(RabbitMQ)
        │   ├── OrderDTO.java                  // 订单详情 DTO
        │   ├── Request.java                   // 秒杀请求
        │   └── Response.java                  // 秒杀响应
        ├── entity/
        │   ├── Goods.java                     // 商品实体 (seckill_goods)
        │   ├── Order.java                     // 订单实体 (seckill_order)
        │   └── User.java                      // 用户实体 (seckill_user)
        ├── exception/
        │   ├── GlobalExceptionHandler.java    // 全局异常处理
        │   └── SeckillException.java          // 业务异常
        ├── mapper/
        │   ├── GoodsMapper.java               // 商品 Mapper
        │   ├── OrderMapper.java               // 订单 Mapper
        │   └── UserMapper.java                // 用户 Mapper
        ├── result/
        │   └── Result.java                    // 统一响应包装
        ├── service/
        │   ├── GoodsService.java              // 商品服务接口
        │   ├── OrderService.java              // 订单服务接口
        │   ├── SeckillService.java            // 秒杀服务接口
        │   ├── UserService.java               // 用户服务接口
        │   ├── demo/
        │   │   ├── DistributedLockService.java // 分布式锁服务(Redis)
        │   │   ├── RateLimitService.java       // 频率限流服务(Redis)
        │   │   ├── RedisStockService.java      // Redis 库存服务
        │   │   ├── SeckillMessageConsumer.java // MQ 消费者(异步下单)
        │   │   └── SeckillMessageProducer.java // MQ 生产者
        │   └── Serviceimpl/
        │       ├── GoodsServiceImpl.java       // 商品服务实现
        │       ├── OrderServiceImpl.java       // 订单服务实现
        │       ├── SeckillServiceImpl.java     // 秒杀服务实现(核心)
        │       ├── UserOrderCheckService.java  // 一人一单检查服务
        │       └── UserServiceImpl.java        // 用户服务实现
        └── util/
            ├── JwtUtil.java                   // JWT 工具类
            ├── Md5Util.java                   // MD5 工具类
            └── OrderNoGenerator.java          // 订单号生成器
```

---

## 11. API 汇总表

| 方法 | 路径 | 鉴权 | 功能 |
|------|------|------|------|
| POST | `/api/user/login` | 否 | 用户登录 |
| GET | `/api/user/info` | 是 | 获取当前用户信息 |
| GET | `/api/user/validate` | 是 | 验证 Token 有效性 |
| GET | `/api/goods/list` | 否 | 获取全部商品 |
| GET | `/api/goods/ongoing` | 否 | 获取进行中的秒杀商品 |
| GET | `/api/goods/coming-soon` | 否 | 获取即将开始的秒杀商品 |
| GET | `/api/goods/{goodsId}` | 否 | 获取商品详情 |
| GET | `/api/goods/{goodsId}/stock` | 否 | 获取商品库存 |
| POST | `/api/seckill/execute` | 是 | **执行秒杀**（核心接口） |
| GET | `/api/seckill/result/{orderNo}` | 是 | 查询秒杀结果 |
| GET | `/api/order/list` | 是 | 获取用户订单列表 |
| GET | `/api/order/{orderNo}` | 是 | 获取订单详情 |
| POST | `/api/order/{orderNo}/pay` | 是 | 支付订单 |
| POST | `/api/order/{orderNo}/cancel` | 是 | 取消订单 |

共 **14 个 API 端点**，其中 9 个需要 JWT Token 鉴权。
