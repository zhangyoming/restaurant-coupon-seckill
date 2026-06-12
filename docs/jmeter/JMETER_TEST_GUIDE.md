# JMeter 压测指南

> 本文档用于指导你在本地对餐饮门店优惠券秒杀系统进行压测。压测结果会受到机器配置、MySQL、Redis、RabbitMQ、JVM 参数和网络环境影响，请以本地真实结果为准。

## 1. 压测目标

本项目准备了两类压测脚本：

| 脚本 | 目标 |
|---|---|
| `jmeter/01_core_seckill_token_disabled.jmx` | 压测 Redis Lua + RabbitMQ 核心抢券链路 |
| `jmeter/02_full_flow_token_required.jmx` | 压测验证码 + 秒杀令牌 + 限流 + 抢券完整链路 |

核心链路用于看吞吐能力，完整链路用于验证业务流程和防刷能力。

---

## 2. 准备环境

### 2.1 MySQL

```bash
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS restaurant_coupon_seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -proot restaurant_coupon_seckill < sql/01_schema.sql
```

### 2.2 Redis

确认 Redis 正常运行：

```bash
redis-cli PING
```

期望返回：

```text
PONG
```

### 2.3 RabbitMQ

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

管理后台：

```text
http://localhost:15672
账号：guest
密码：guest
```

### 2.4 JMeter

建议使用 JMeter 5.6.x。

验证：

```bash
jmeter -v
```

---

## 3. 初始化压测数据

执行：

```bash
mysql -uroot -proot restaurant_coupon_seckill < sql/02_jmeter_reset_data.sql
```

该 SQL 会创建压测活动：

```text
活动编号：9001
门店编号：901
总库存：100000
活动时间：2026-01-01 到 2036-12-31
状态：已发布
```

清理 Redis：

```bash
redis-cli DEL coupon:activity:9001:info coupon:activity:9001:stock coupon:activity:9001:users coupon:activity:9001:soldout
```

---

## 4. 核心抢券链路压测

### 4.1 启动应用

核心链路压测建议使用 `jmeter` profile：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=jmeter
```

这个 profile 会：

```text
关闭秒杀令牌校验
放宽用户限流
放宽 IP 限流
提高 RabbitMQ 消费并发
```

目的是单独测试：

```text
Redis Lua + RabbitMQ 异步发券核心链路
```

### 4.2 预热 Redis

```bash
curl -X POST "http://127.0.0.1:8081/admin/coupon/activity/preheat?id=9001"
```

查询库存：

```bash
curl "http://127.0.0.1:8081/admin/coupon/activity/redis-stock?id=9001"
```

### 4.3 小并发验证

```bash
jmeter -n -t jmeter/01_core_seckill_token_disabled.jmx \
  -l reports/core_50.jtl \
  -e -o reports/html_core_50 \
  -Jthreads=50 \
  -JrampUp=10 \
  -Jloops=1
```

### 4.4 中等并发

```bash
jmeter -n -t jmeter/01_core_seckill_token_disabled.jmx \
  -l reports/core_200.jtl \
  -e -o reports/html_core_200 \
  -Jthreads=200 \
  -JrampUp=10 \
  -Jloops=1
```

### 4.5 高并发

```bash
jmeter -n -t jmeter/01_core_seckill_token_disabled.jmx \
  -l reports/core_1000.jtl \
  -e -o reports/html_core_1000 \
  -Jthreads=1000 \
  -JrampUp=30 \
  -Jloops=1
```

---

## 5. 完整防刷链路压测

完整链路需要保持：

```yaml
restaurant:
  coupon:
    seckill:
      token-required: true
```

启动：

```bash
mvn spring-boot:run
```

运行：

```bash
jmeter -n -t jmeter/02_full_flow_token_required.jmx \
  -l reports/full_20.jtl \
  -e -o reports/html_full_20 \
  -Jthreads=20 \
  -JrampUp=10 \
  -Jloops=1
```

说明：

```text
完整链路包含获取验证码、解析验证码、创建 token、抢券、查询结果，所以吞吐量一定低于核心链路。
```

---

## 6. 生成 Markdown 汇总

使用分析脚本：

```bash
python tools/analyze_jmeter_result.py reports/core_200.jtl reports/core_200_summary.md
```

生成结果后，把数据复制到：

```text
docs/jmeter/PERFORMANCE_REPORT.md
```

---

## 7. 数据一致性校验

压测完成后执行：

```sql
SELECT COUNT(*) AS record_count
FROM coupon_record
WHERE activity_id = 9001;

SELECT COUNT(*) AS success_order_count
FROM coupon_seckill_order
WHERE activity_id = 9001
  AND status = 1;

SELECT total_stock, available_stock
FROM coupon_activity
WHERE id = 9001;

SELECT user_id, COUNT(*) AS cnt
FROM coupon_record
WHERE activity_id = 9001
GROUP BY user_id
HAVING cnt > 1;
```

应满足：

```text
record_count = success_order_count
record_count + available_stock = total_stock
重复领取查询结果为空
```

---

## 8. 常见问题

### 8.1 JMeter 大量失败

检查：

```text
1. 应用是否启动
2. Redis 是否启动
3. RabbitMQ 是否启动
4. 活动 9001 是否存在
5. Redis 库存是否已经预热
6. 是否被限流或 token 校验拦截
```

### 8.2 RabbitMQ 消费很慢

检查：

```text
1. RabbitMQ 控制台队列是否堆积
2. application-jmeter.yml 的消费者并发数
3. MySQL 连接池是否成为瓶颈
4. 数据库机器 CPU 和 IO 是否过高
```

### 8.3 结果查询一直排队中

说明消费者可能没有正常消费。

检查：

```text
1. RabbitMQ 队列是否有消息堆积
2. CouponSeckillConsumer 是否启动
3. 控制台是否有消费异常日志
4. MySQL 是否有死锁或唯一索引冲突异常
```

### 8.4 压测数据不一致

检查：

```text
1. 是否在压测前执行了 sql/02_jmeter_reset_data.sql
2. 是否清理了 Redis 活动库存和已领取用户集合
3. 是否有上一次压测遗留数据
4. 是否手动多次执行了抢券接口
```

---

## 9. 建议压测策略

不要一开始就 1000 并发。

建议顺序：

```text
50 并发：验证脚本和业务正确性
200 并发：观察基础性能
500 并发：观察 Redis、RabbitMQ、MySQL 压力
1000 并发：观察瓶颈和错误率
```

压测时建议打开：

```text
RabbitMQ 控制台
Redis 监控工具
MySQL 连接数监控
应用控制台日志
JMeter HTML 报告
```
