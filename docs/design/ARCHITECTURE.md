# 架构设计说明

## 1. 项目定位

餐饮门店优惠券秒杀系统是一个后端高并发项目，重点解决限量优惠券在大量用户同时抢券时的超卖、重复领取、数据库压力和接口防刷问题。

本项目不是完整商城系统，也不是普通优惠券 CRUD，而是聚焦秒杀核心链路。

---

## 2. 整体架构

```text
客户端 / JMeter / 接口工具
        ↓
Spring Boot Web Controller
        ↓
参数校验 + 活动校验 + 验证码 + 秒杀令牌 + 限流
        ↓
Redis Lua 原子扣库存
        ↓
创建排队订单
        ↓
RabbitMQ 异步消息
        ↓
消费者异步发券
        ↓
MySQL 落库：扣库存、创建领取记录、更新订单
        ↓
Redis 写入抢券结果
        ↓
用户查询结果
```

---

## 3. 分层结构

```text
controller
├── admin    管理端接口：门店、活动、领取记录
└── app      用户端接口：活动列表、验证码、令牌、抢券、我的优惠券

service
├── store    门店业务
├── activity 活动业务
├── cache    Redis 库存预热、Lua 扣库存、结果缓存
├── access   验证码、秒杀令牌、限流、防刷
├── seckill  抢券主流程、异步发券处理
└── record   领取记录业务

mq
├── config   RabbitMQ 交换机、队列、消息转换器配置
├── producer 抢券消息生产者
├── consumer 抢券消息消费者
└── message  MQ 消息体

dal
├── dataobject 数据库实体
└── mapper     MyBatis Plus Mapper
```

---

## 4. 核心链路设计

### 4.1 活动发布与库存预热

```text
运营创建活动
   ↓
活动状态为草稿
   ↓
发布活动
   ↓
事务提交后预热 Redis
   ↓
写入库存、售罄标记、已领取用户集合
```

设计原因：

```text
活动创建阶段不允许被用户抢券，只有发布后才预热 Redis。
事务提交后预热 Redis，避免数据库回滚但 Redis 已写入脏数据。
```

---

### 4.2 验证码与秒杀令牌

```text
用户获取验证码
   ↓
后端生成算术表达式和答案
   ↓
答案写入 Redis，设置过期时间
   ↓
用户提交答案
   ↓
答案正确则生成短期秒杀令牌
   ↓
抢券接口必须携带 token
```

设计原因：

```text
降低机器人直接刷抢券接口的概率。
避免用户在活动开始前提前构造抢券请求。
通过短期、一次性 token 提高接口防刷能力。
```

---

### 4.3 Redis Lua 原子扣库存

Lua 脚本完成：

```text
1. 判断库存 Key 是否存在
2. 判断库存是否大于 0
3. 判断用户是否已领取
4. 扣减 Redis 库存
5. 把用户加入已领取集合
```

设计原因：

```text
Redis 单条命令是原子的，但多条命令组合不是原子的。
Lua 脚本在 Redis 服务端整体执行，可以保证库存判断、重复领取判断、扣库存和记录用户的原子性。
```

---

### 4.4 RabbitMQ 异步发券

```text
Redis Lua 成功
   ↓
创建排队中订单
   ↓
事务提交后发送 MQ
   ↓
接口返回排队中
   ↓
消费者异步处理
   ↓
扣减数据库库存
   ↓
创建领取记录
   ↓
更新订单为成功
```

设计原因：

```text
高并发场景下，同步写数据库会拉长接口耗时并增加数据库压力。
MQ 可以削峰填谷，把突发请求转化为队列中的平稳消费。
```

---

## 5. 数据一致性设计

### 5.1 防超卖

| 层级 | 方案 |
|---|---|
| Redis | Lua 判断库存并预扣库存 |
| MySQL | `UPDATE coupon_activity SET available_stock = available_stock - 1 WHERE id = ? AND available_stock > 0` |

说明：

```text
Redis Lua 负责挡住绝大多数高并发请求。
MySQL 条件扣库存作为最终兜底，确保数据库库存不为负。
```

### 5.2 防重复领取

| 层级 | 方案 |
|---|---|
| Redis | 已领取用户 Set |
| 订单表 | `user_id + activity_id` 唯一索引 |
| 领取记录表 | `user_id + activity_id` 唯一索引 |

说明：

```text
Redis 提前拦截重复请求，数据库唯一索引兜底。
```

### 5.3 MQ 重复消费幂等

```text
消费者只处理 status = 0 的排队中订单。
如果订单已经成功或失败，重复消息直接忽略。
```

### 5.4 Redis 预扣库存补偿

```text
Redis Lua 成功后，如果数据库落库失败：
1. 从 Redis 已领取用户集合移除该用户
2. 如果移除成功，则 Redis 库存 +1
3. 如果库存恢复为正数，则 soldout 标记改回 false
```

---

## 6. 限流设计

使用 Redis Lua 实现固定窗口限流。

限流对象：

```text
抢券接口：用户维度、IP 维度
创建令牌接口：用户维度、IP 维度
获取验证码接口：用户维度、IP 维度
```

固定窗口逻辑：

```text
INCR key
如果是第一次访问，设置过期时间
如果计数超过阈值，拒绝请求
```

设计原因：

```text
实现简单，适合实习项目和单体服务。
如果后续继续增强，可以升级为滑动窗口或令牌桶。
```

---

## 7. 数据库表设计

| 表 | 作用 |
|---|---|
| `restaurant_store` | 餐饮门店 |
| `coupon_activity` | 优惠券活动和库存 |
| `coupon_record` | 用户优惠券领取记录 |
| `coupon_seckill_order` | 秒杀请求订单和处理状态 |

关键字段：

```text
coupon_activity.available_stock：数据库剩余库存
coupon_activity.total_stock：总库存
coupon_record.user_id + activity_id：防重复领取
coupon_seckill_order.status：排队中、成功、失败
coupon_seckill_order.request_id：请求编号
```

---

## 8. Redis Key 设计

详见 README 的 Redis Key 表。

关键思想：

```text
活动库存放 Redis String
已领取用户放 Redis Set
抢券结果放 Redis String
验证码和秒杀令牌放 Redis String 并设置过期时间
限流计数放 Redis String 并设置窗口过期时间
```

---

## 9. 可扩展点

后续可以扩展：

```text
1. 接入登录系统，用真实 userId 替代请求参数 userId。
2. 把固定窗口限流升级成滑动窗口限流。
3. 增加死信队列处理消费失败消息。
4. 增加 Prometheus + Grafana 监控 Redis、RabbitMQ、JVM、接口耗时。
5. 增加简单 Vue3 用户抢券演示页。
6. 增加优惠券核销功能，形成抢券到到店消费闭环。
```
