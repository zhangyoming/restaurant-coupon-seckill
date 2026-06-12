# 餐饮门店优惠券秒杀系统演示流程

> 本文档用于面试演示、录屏演示、GitHub README 引导。建议演示前先启动 MySQL、Redis、RabbitMQ 和后端服务。

## 1. 演示目标

通过一条完整链路证明系统不是普通 CRUD，而是完整的高并发抢券系统：

```text
创建门店
  -> 创建优惠券活动
  -> 发布活动并预热 Redis
  -> 用户获取验证码
  -> 用户创建秒杀令牌
  -> 用户抢券
  -> Redis Lua 原子扣库存
  -> RabbitMQ 异步发券
  -> 用户查询结果
  -> 数据库一致性校验
```

---

## 2. 演示前准备

### 2.1 启动 MySQL

创建数据库并执行建表 SQL：

```bash
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS restaurant_coupon_seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -proot restaurant_coupon_seckill < sql/01_schema.sql
```

### 2.2 启动 Redis

确认 Redis 运行在：

```text
127.0.0.1:6379
```

### 2.3 启动 RabbitMQ

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

RabbitMQ 控制台：

```text
http://localhost:15672
账号：guest
密码：guest
```

### 2.4 启动后端

```bash
mvn spring-boot:run
```

Knife4j：

```text
http://localhost:8081/doc.html
```

---

## 3. 演示流程一：管理端创建活动

### 第 1 步：创建门店

接口：

```http
POST /admin/store/create
```

请求：

```json
{
  "name": "春熙路火锅店",
  "address": "成都市锦江区春熙路 88 号",
  "phone": "028-88888888",
  "status": 0
}
```

讲解点：

```text
门店是餐饮业务的基础维度，后续优惠券活动会绑定具体门店。
```

### 第 2 步：创建优惠券活动

接口：

```http
POST /admin/coupon/activity/create
```

请求：

```json
{
  "storeId": 1,
  "title": "春熙路火锅店开业抢券",
  "couponName": "满 100 减 30 优惠券",
  "couponAmount": 30.00,
  "thresholdAmount": 100.00,
  "totalStock": 1000,
  "perUserLimit": 1,
  "startTime": "2026-07-01T12:00:00",
  "endTime": "2036-07-01T13:00:00"
}
```

讲解点：

```text
活动创建时库存先写入 MySQL，发布后才会预热到 Redis，避免草稿活动被提前抢。
```

### 第 3 步：发布活动

接口：

```http
PUT /admin/coupon/activity/publish?id=1001
```

讲解点：

```text
发布活动后会在事务提交后自动预热 Redis 库存，避免数据库回滚但 Redis 已经写入的问题。
```

### 第 4 步：查询 Redis 库存

接口：

```http
GET /admin/coupon/activity/redis-stock?id=1001
```

预期：

```text
返回库存数量，例如 1000。
```

讲解点：

```text
用户抢券时优先走 Redis，不直接访问数据库库存，减少高并发下数据库压力。
```

---

## 4. 演示流程二：用户抢券

### 第 1 步：获取验证码

接口：

```http
GET /app/coupon/seckill/captcha?userId=20001&activityId=1001
```

预期返回：

```json
{
  "captchaId": "xxx",
  "expression": "7 + 2 = ?",
  "expireSeconds": 120
}
```

讲解点：

```text
验证码用于增加机器刷接口成本，后端只返回表达式，不返回答案。
```

### 第 2 步：创建秒杀令牌

接口：

```http
POST /app/coupon/seckill/token
```

请求：

```json
{
  "userId": 20001,
  "activityId": 1001,
  "captchaId": "上一步返回的 captchaId",
  "captchaCode": "算出来的答案"
}
```

预期返回：

```json
{
  "token": "xxx",
  "expireSeconds": 60
}
```

讲解点：

```text
秒杀令牌短期有效、一次性使用，并且绑定 userId 和 activityId，可以防止用户提前构造请求或重复提交。
```

### 第 3 步：携带 token 抢券

接口：

```http
POST /app/coupon/seckill
```

请求：

```json
{
  "userId": 20001,
  "activityId": 1001,
  "token": "上一步返回的 token"
}
```

预期返回：

```json
{
  "requestId": "xxx",
  "orderId": 1,
  "resultCode": "QUEUING",
  "resultMsg": "排队中，请稍后查询结果"
}
```

讲解点：

```text
抢券接口不直接发券，只做 Redis Lua 预扣库存并创建排队订单，然后投递 RabbitMQ，立即返回排队中。
```

### 第 4 步：查询抢券结果

接口：

```http
GET /app/coupon/seckill/result?userId=20001&activityId=1001
```

预期：

```text
短时间内可能是 QUEUING，消费者处理后变成 SUCCESS 或 FAILED。
```

讲解点：

```text
秒杀系统通常不直接同步返回最终成功，而是通过轮询查询异步处理结果，避免高并发请求长时间阻塞。
```

### 第 5 步：查询我的优惠券

接口：

```http
GET /app/coupon/my/page?userId=20001&pageNo=1&pageSize=10
```

预期：

```text
可以看到用户已领取的优惠券。
```

---

## 5. 演示流程三：重复领取拦截

用同一个用户再次走抢券流程。

预期结果：

```text
请勿重复领取该优惠券
```

讲解点：

```text
系统在 Redis Lua 层用 Set 判断用户是否已经领取，同时 MySQL 通过 user_id + activity_id 唯一索引兜底，防止重复领取。
```

---

## 6. 演示流程四：限流与防刷

连续快速调用抢券接口或创建令牌接口。

预期结果：

```text
当前用户请求过于频繁，请稍后再试
当前 IP 请求过于频繁，请稍后再试
```

讲解点：

```text
系统使用 Redis Lua 固定窗口限流，保证 INCR + EXPIRE 原子执行，可以在多实例部署下共享限流状态。
```

---

## 7. 演示流程五：RabbitMQ 异步消费

打开 RabbitMQ 管理后台：

```text
http://localhost:15672
```

查看：

```text
Exchange: coupon.seckill.exchange
Queue: coupon.seckill.queue
```

讲解点：

```text
抢券接口只负责快速校验和投递消息，发券落库由消费者异步完成。这样可以削峰填谷，避免高并发请求直接同步写数据库。
```

---

## 8. 演示流程六：数据一致性校验

执行 SQL：

```sql
SELECT COUNT(*) AS record_count
FROM coupon_record
WHERE activity_id = 1001;

SELECT COUNT(*) AS success_order_count
FROM coupon_seckill_order
WHERE activity_id = 1001
  AND status = 1;

SELECT total_stock, available_stock
FROM coupon_activity
WHERE id = 1001;
```

检查：

```text
领取记录数 = 成功订单数
领取记录数 + 剩余库存 = 总库存
```

讲解点：

```text
Redis Lua 负责高并发入口保护，MySQL 唯一索引和库存条件更新作为最终兜底，保证最终不超卖、不重复发券。
```

---

## 9. 录屏演示建议

建议录屏顺序：

```text
1. 打开 README，说明项目定位
2. 打开 Knife4j，展示接口分组
3. 创建门店和活动
4. 发布活动并查询 Redis 库存
5. 用户获取验证码、创建 token、抢券
6. 查询抢券结果和我的优惠券
7. 展示 RabbitMQ 队列
8. 执行 SQL 校验数据一致性
9. 展示 JMeter 压测报告模板
```

控制在 5 到 8 分钟即可。
