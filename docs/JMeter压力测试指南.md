# JMeter 秒杀系统压力测试指南

## 核心问题

秒杀接口 `POST /api/seckill/execute` 需要 `Authorization` header 携带 JWT Token，
Token 内绑定了 `userId`，因此**每个虚拟用户必须使用独立的 Token**。

## 架构概览

```
┌─────────────┐     ┌──────────────┐     ┌──────────────┐
│   JMeter    │────▶│  Spring Boot │────▶│    Redis     │
│  N个线程    │     │   :8080      │     │  (库存扣减)   │
│  独立Token  │     │              │     └──────────────┘
└─────────────┘     │              │     ┌──────────────┐
                    │              │────▶│  RabbitMQ    │
                    └──────────────┘     │  (异步下单)   │
                                         └──────────────┘
```

---

## 方案 A：CSV 数据驱动（推荐，简单可靠）

### 原理
预先生成所有用户的 Token → 写入 CSV → JMeter 每个线程从 CSV 读取一行

### 步骤

#### 1. 批量创建测试用户

```bash
# 在 MySQL 中执行
mysql -u root -p < sql/batch_create_users.sql
```

这会创建 1000 个测试用户 `perfuser0001` ~ `perfuser1000`，密码均为 `123456`。

#### 2. 导出 Token CSV

```bash
# 启动应用后执行
curl -s http://localhost:8080/api/admin/export-tokens?prefix=perfuser > /tmp/tokens.csv

# 查看生成结果
head -5 /tmp/tokens.csv
# 输出示例:
# userId,username,token
# 4,perfuser0001,eyJhbGciOiJIUzUxMiJ9...
# 5,perfuser0002,eyJhbGciOiJIUzUxMiJ9...
# ...
```

#### 3. JMeter 配置

##### 3.1 Thread Group（线程组）
```
Number of Threads:    200          ← 并发用户数
Ramp-up period:       20           ← 20秒内全部启动
Loop Count:           5            ← 每个用户循环5次
→ 总共 1000 次请求
```

##### 3.2 CSV Data Set Config
```
Filename:              /tmp/tokens.csv
File Encoding:         UTF-8
Variable Names:        userId,username,token
Ignore first line:     True         ← 跳过表头
Delimiter:             ,
Recycle on EOF:        True         ← CSV读完从头循环
Stop thread on EOF:    False
Sharing mode:          Current thread  ← 重要!每个线程独立读取
```

##### 3.3 HTTP Request Defaults
```
Protocol:              http
Server Name:           localhost
Port Number:           8080
```

##### 3.4 HTTP Header Manager
```
Authorization:         Bearer ${token}
Content-Type:          application/json
```

##### 3.5 HTTP Request（秒杀接口）
```
Method:                POST
Path:                  /api/seckill/execute
Body Data:             {"goodsId":3}
```
> **注意**: goodsId 要选择一个 `status=2`（进行中）的商品。查 SQL：
> ```sql
> SELECT id, goods_name, status, stock FROM seckill_goods WHERE status = 2;
> ```

##### 3.6 监听器
```
View Results Tree      ← 调试时用（正式压测时关掉）
Summary Report         ← 查看吞吐量
Aggregate Report       ← 查看响应时间分布
```

#### 4. 完整 JMeter 组件树
```
Test Plan
├── CSV Data Set Config
├── Thread Group
│   ├── HTTP Request Defaults
│   ├── HTTP Header Manager
│   ├── HTTP Request - Seckill (POST /api/seckill/execute)
│   └── HTTP Request - Result  (GET /api/seckill/result/${orderNo})
├── Aggregate Report
└── View Results Tree
```

---

## 方案 B：JSR223 PreProcessor 动态生成 Token（零文件）

### 原理
在 JMeter 中嵌入 Groovy 脚本，使用与后端相同的密钥实时生成 Token

### 步骤

#### 1. 添加 jjwt 依赖到 JMeter

```bash
# 找到 jjwt jar 包（在 Maven 本地仓库）
MAVEN_REPO=~/.m2/repository

# 复制到 JMeter lib 目录
cp $MAVEN_REPO/io/jsonwebtoken/jjwt-api/0.11.5/jjwt-api-0.11.5.jar \
   $MAVEN_REPO/io/jsonwebtoken/jjwt-impl/0.11.5/jjwt-impl-0.11.5.jar \
   $MAVEN_REPO/io/jsonwebtoken/jjwt-jackson/0.11.5/jjwt-jackson-0.11.5.jar \
   /path/to/jmeter/lib/

# 重启 JMeter
```

#### 2. JSR223 PreProcessor 配置

在 HTTP Request 上右键 → Add → Pre Processors → JSR223 PreProcessor

```
Language:              groovy
```

脚本内容：

```groovy
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import javax.crypto.SecretKey

// ====== 与后端 JwtUtil 相同的密钥 ======
String SECRET_BASE64 = "Ocr4iVbAHZiI0rwSwx5TYwBeIK54TY47E1vgU+O/hUeMjbJRE01K14yzwkt+58JggPxIKA9sTUxfUTuXBEk5NQ=="
SecretKey SECRET_KEY = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64))

// ====== 计算当前用户信息 ======
// 方案1: 基于线程号
int threadNum = ctx.getThreadNum()       // 0-based
Long userId = 4L + threadNum              // 假设压测用户从 ID=4 开始
String username = "perfuser" + String.format("%04d", threadNum + 1)

// 方案2: 基于 JMeter 变量（配合 Counter 元件）
// Long userId = Long.parseLong(vars.get("userId"))
// String username = vars.get("username")

// ====== 生成 Token ======
String token = Jwts.builder()
    .claim("userId", userId)
    .claim("username", username)
    .setSubject(username)
    .setIssuedAt(new Date())
    .setExpiration(new Date(System.currentTimeMillis() + 86400000L))
    .signWith(SECRET_KEY)
    .compact()

// 写入 JMeter 变量
vars.put("token", token)
vars.put("userId", String.valueOf(userId))
vars.put("username", username)

log.info("Generated token for userId=" + userId)
```

#### 3. 然后在 HTTP Header Manager 中使用
```
Authorization:         Bearer ${token}
```

### 优缺点对比

| 维度 | 方案 A (CSV) | 方案 B (JSR223) |
|------|-------------|-----------------|
| 设置复杂度 | ⭐ 简单 | ⭐⭐⭐ 需要配置 jar |
| 灵活性 | 中等 | 高 |
| Token 真实性 | 真实用户 | 真实用户 |
| 性能开销 | 极低 | 每次请求生成 JWT（微乎其微） |
| 调试难度 | 低 | 中等 |

---

## 进阶：阶梯式压测（Stepping Thread Group）

如果你想做阶梯式加压，JMeter 需要安装 **Custom Thread Groups** 插件：

```bash
# 下载 jmeter-plugins-manager.jar 放到 lib/ext/
# 然后在 JMeter 中: Options → Plugins Manager → 搜索 "Custom Thread Groups"
```

配置示例（模拟真实秒杀场景——瞬时高峰）：

```
Thread Group:    Concurrency Thread Group
Target Concurrency:   500
Ramp Up Time:         5 sec         ← 5秒内冲到500并发
Hold Target Rate:     30 sec        ← 维持30秒
Ramp Down:            5 sec
```

---

## 压测时需要注意的问题

### 1. 确保有"进行中"的商品
```sql
-- 查看商品状态，需要 status=2 才能秒杀
SELECT id, goods_name, status, stock, start_time, end_time
FROM seckill_goods;

-- 如果没有进行中的，手动更新
UPDATE seckill_goods SET status = 2, start_time = NOW(),
    end_time = DATE_ADD(NOW(), INTERVAL 1 DAY)
WHERE id = 3;
```

### 2. Redis 库存预热
确保应用启动后 `InitGoods` 已经把库存加载到了 Redis：
```bash
redis-cli
> KEYS seckill:stock:*
> GET seckill:stock:3
```

### 3. 数据库连接池
```yaml
# application.yml 中调整
hikari:
  maximum-pool-size: 50   ← 压测时适当加大
```

### 4. 关闭日志
压测时把日志级别调高，减少磁盘 IO：
```yaml
logging:
  level:
    com.seckill: warn     ← 从 debug 改为 warn
```

### 5. 关注指标
| 指标 | 含义 | 健康值 |
|------|------|--------|
| Throughput | 每秒处理请求数 | 越高越好 |
| Avg Response Time | 平均响应时间 | < 500ms |
| 99% Line | 99%请求的响应时间 | < 2s |
| Error % | 错误率 | < 1% |

### 6. 观察系统瓶颈
```bash
# Redis 连接数
redis-cli INFO clients

# MySQL 连接数
mysql -e "SHOW PROCESSLIST;"

# JVM GC
jstat -gcutil <pid> 1000
```

---

## 快速启动清单

```bash
# 1. 创建测试用户
mysql -u root -p < sql/batch_create_users.sql

# 2. 启动应用
mvn spring-boot:run

# 3. 确保有进行中的商品（用SQL检查/更新）

# 4. 导出 Token CSV
curl -s http://localhost:8080/api/admin/export-tokens?prefix=perfuser > tokens.csv

# 5. 打开 JMeter，导入 tokens.csv 路径，配置 Thread Group

# 6. 运行压测
```
