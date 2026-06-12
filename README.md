# 餐饮门店优惠券秒杀系统

> 一个面向餐饮门店营销场景的高并发优惠券抢购系统。项目围绕“限量优惠券抢购”这一真实业务，重点解决高并发场景下的库存超卖、重复领取、接口防刷、异步削峰、结果查询与压测验证问题。

## 1. 项目简介

餐饮门店在新店开业、节假日营销、会员促活时，经常会发放限量优惠券，例如：

- 新店开业满 100 减 30 券
- 午市限量 5 折券
- 霸王餐体验券
- 节假日限时抢购券

如果直接让所有请求打到数据库，容易出现以下问题：

- 大量并发请求瞬间冲击 MySQL，数据库压力过大；
- 多个用户同时扣减库存，可能导致优惠券超卖；
- 同一个用户可能重复请求，造成重复领取；
- 恶意用户提前构造请求，绕过前端按钮限制；
- 抢券接口同步写数据库，响应慢且不利于削峰；
- 系统是否真的不超卖、不重复发券缺少压测与数据校验。

本项目基于 Spring Boot、Redis、Lua、RabbitMQ、MySQL 和 Vue3 实现餐饮门店优惠券秒杀系统。系统通过 Redis 库存预热、Lua 原子扣库存、RabbitMQ 异步发券、秒杀令牌、接口限流和 JMeter 压测，构建一套完整的高并发抢券链路。

## 2. 项目定位

本项目不是普通优惠券 CRUD，而是一个偏后端高并发能力展示的 Java 实习项目。

它适合作为简历中的第二项目，与“餐饮共享股东分红管理系统”形成互补：

| 项目 | 方向 | 主要体现能力 |
|---|---|---|
| 餐饮共享股东分红管理系统 | B 端经营管理 / 财务分红 | 业务建模、权限控制、分红计算、审批、看板、数据权限 |
| 餐饮门店优惠券秒杀系统 | C 端高并发营销 | Redis、Lua、RabbitMQ、限流、防刷、防超卖、压测 |

## 3. 技术栈

### 后端技术

| 技术 | 用途 |
|---|---|
| Java 17 | 后端开发语言 |
| Spring Boot 3.x | 后端基础框架 |
| Spring Web | RESTful 接口开发 |
| Spring Validation | 请求参数校验 |
| MyBatis Plus | 数据库 ORM 与分页查询 |
| MySQL | 活动、订单、领取记录等核心数据存储 |
| Redis | 库存预热、用户领取集合、秒杀结果、验证码、令牌、限流 |
| Redis Lua | 原子校验库存、重复领取和扣减库存 |
| RabbitMQ | 异步发券、削峰填谷 |
| Knife4j / OpenAPI | 接口文档 |
| JMeter | 秒杀接口压测 |
| Docker | RabbitMQ 等中间件运行环境 |

### 前端技术

| 技术 | 用途 |
|---|---|
| Vue 3 | 抢券演示页面 |
| TypeScript | 类型约束 |
| Vite | 前端构建工具 |
| Axios | 调用后端接口 |
| 原生 CSS | 简单页面样式 |

## 4. 核心功能

### 4.1 管理端功能

- 门店管理
  - 新增门店
  - 修改门店
  - 删除门店
  - 门店分页查询

- 优惠券活动管理
  - 创建优惠券活动
  - 修改优惠券活动
  - 发布活动
  - 下架活动
  - 手动预热 Redis 库存
  - 查询 Redis 库存
  - 活动分页查询

- 领取记录管理
  - 查看用户领取记录
  - 按活动、用户、门店查询领取记录

### 4.2 用户端功能

- 查看可抢优惠券活动
- 获取算术验证码
- 校验验证码并创建秒杀令牌
- 携带秒杀令牌抢券
- 查询抢券结果
- 查看我的优惠券

### 4.3 高并发增强功能

- Redis 库存预热
- Redis Lua 原子扣库存
- RabbitMQ 异步发券
- 用户维度限流
- IP 维度限流
- 验证码防刷
- 秒杀令牌防提前请求
- 一人一券控制
- MQ 消费幂等
- Redis 预扣库存补偿
- MySQL 唯一索引兜底
- JMeter 压测与性能报告

## 5. 系统架构

```text
用户 / 前端页面
   ↓
Vue3 抢券演示页
   ↓
Spring Boot 接口层
   ↓
参数校验 / 验证码 / 秒杀令牌 / 用户限流 / IP 限流
   ↓
Redis Lua 原子校验库存和重复领取
   ↓
创建排队订单
   ↓
发送 RabbitMQ 消息
   ↓
接口立即返回“排队中”
   ↓
RabbitMQ 消费者异步发券
   ↓
MySQL 扣减库存 + 创建领取记录 + 更新订单状态
   ↓
Redis 写入抢券结果
   ↓
用户轮询查询结果
```

## 6. 核心业务流程

### 6.1 活动发布与库存预热流程

```text
运营创建优惠券活动
   ↓
设置门店、券名称、库存、开始时间、结束时间
   ↓
发布活动
   ↓
系统将活动库存写入 Redis
   ↓
初始化已领取用户集合
   ↓
初始化售罄标记
   ↓
活动进入可抢状态
```

Redis 预热后会生成类似 Key：

```text
coupon:activity:{activityId}:info
coupon:activity:{activityId}:stock
coupon:activity:{activityId}:users
coupon:activity:{activityId}:soldout
```

### 6.2 用户抢券流程

```text
用户进入活动列表
   ↓
选择活动并点击抢券
   ↓
获取算术验证码
   ↓
输入验证码答案
   ↓
创建短期一次性秒杀令牌
   ↓
携带 token 请求抢券接口
   ↓
后端校验 token、限流、活动状态
   ↓
执行 Redis Lua 脚本
   ↓
Redis 预扣库存成功
   ↓
创建排队订单
   ↓
发送 RabbitMQ 消息
   ↓
接口返回“排队中”
   ↓
消费者异步发券
   ↓
用户轮询查询结果
```

### 6.3 RabbitMQ 异步发券流程

```text
抢券接口投递 MQ 消息
   ↓
消费者监听 coupon.seckill.queue
   ↓
查询秒杀订单
   ↓
判断订单是否仍为“排队中”
   ↓
数据库条件扣减库存
   ↓
插入 coupon_record 领取记录
   ↓
更新订单状态为“成功”
   ↓
写入 Redis 抢券结果 SUCCESS
```

如果发券失败：

```text
更新订单状态为失败
   ↓
记录失败原因
   ↓
补偿 Redis 预扣库存
   ↓
写入 Redis 抢券结果 FAILED
```

## 7. 防超卖设计

系统通过三层机制防止超卖。

### 7.1 第一层：Redis Lua 原子扣库存

Lua 脚本在 Redis 服务端一次性完成：

```text
判断库存是否存在
判断库存是否大于 0
判断用户是否已经抢过
扣减库存
将用户加入已领取集合
```

这样避免了多个 Redis 命令分开执行时出现并发问题。

### 7.2 第二层：MySQL 条件扣库存

消费者落库时仍然执行：

```sql
UPDATE coupon_activity
SET available_stock = available_stock - 1
WHERE id = ?
  AND available_stock > 0;
```

即使 Redis 出现异常，数据库也不会把库存扣成负数。

### 7.3 第三层：数据一致性校验

压测后执行：

```sql
SELECT COUNT(*) AS record_count
FROM coupon_record
WHERE activity_id = 9001;

SELECT COUNT(*) AS success_order_count
FROM coupon_seckill_order
WHERE activity_id = 9001
  AND status = 1;

SELECT available_stock
FROM coupon_activity
WHERE id = 9001;
```

理想结果：

```text
领取记录数 = 成功订单数
领取记录数 + 剩余库存 = 总库存
```

## 8. 防重复领取设计

系统通过四层机制防止同一用户重复领取同一个活动优惠券。

### 8.1 Redis Set 判断

Redis Key：

```text
coupon:activity:{activityId}:users
```

Lua 脚本中通过 `SISMEMBER` 判断用户是否已经抢过。

### 8.2 秒杀订单唯一索引

```sql
UNIQUE KEY uk_user_activity (user_id, activity_id)
```

保证一个用户对同一个活动只能创建一条秒杀订单。

### 8.3 领取记录唯一索引

```sql
UNIQUE KEY uk_user_activity (user_id, activity_id)
```

保证同一用户同一活动只能有一条优惠券领取记录。

### 8.4 MQ 消费幂等

消费者处理消息前判断订单状态：

```text
只有 status = 0（排队中）的订单才允许处理
如果订单已经成功或失败，说明消息重复消费，直接忽略
```

## 9. 接口限流与防刷设计

### 9.1 验证码

用户抢券前需要先获取算术验证码，例如：

```text
7 + 2 = ?
```

后端只返回表达式，不返回答案。答案存入 Redis，设置短期过期时间。

### 9.2 秒杀令牌

用户输入正确验证码后，后端创建秒杀令牌。

令牌特点：

- 绑定用户 ID；
- 绑定活动 ID；
- 有效期短；
- 一次性使用；
- 抢券接口必须携带。

### 9.3 用户限流与 IP 限流

Redis Key 示例：

```text
coupon:limit:seckill:user:{userId}
coupon:limit:seckill:ip:{clientIp}
```

通过 Redis Lua 固定窗口限流，限制单个用户或单个 IP 在短时间内的访问次数。

## 10. RabbitMQ 设计

### 10.1 交换机、队列、路由键

| 名称 | 值 |
|---|---|
| Exchange | `coupon.seckill.exchange` |
| Queue | `coupon.seckill.queue` |
| Routing Key | `coupon.seckill.create` |

### 10.2 消息体

```java
public class CouponSeckillMessage {
    private Long userId;
    private Long activityId;
    private Long storeId;
    private Long orderId;
    private String requestId;
    private LocalDateTime requestTime;
}
```

### 10.3 为什么使用 RabbitMQ

抢券请求高峰时，如果接口同步扣数据库库存、插入领取记录，会导致数据库瞬间压力过大。使用 RabbitMQ 后，抢券接口只负责快速校验和投递消息，真正的发券操作由消费者异步执行。

优点：

- 削峰填谷；
- 降低接口响应时间；
- 降低数据库瞬时压力；
- 消费者可以控制并发消费速度；
- 失败场景可以记录订单状态并补偿。

## 11. 数据库表设计

### 11.1 门店表：restaurant_store

存储餐饮门店基础信息。

主要字段：

| 字段 | 说明 |
|---|---|
| id | 门店编号 |
| name | 门店名称 |
| address | 门店地址 |
| status | 状态 |
| create_time | 创建时间 |
| update_time | 更新时间 |
| deleted | 逻辑删除 |

### 11.2 优惠券活动表：coupon_activity

存储优惠券秒杀活动。

主要字段：

| 字段 | 说明 |
|---|---|
| id | 活动编号 |
| store_id | 门店编号 |
| title | 活动标题 |
| coupon_name | 优惠券名称 |
| coupon_amount | 优惠金额 |
| threshold_amount | 使用门槛 |
| total_stock | 总库存 |
| available_stock | 剩余库存 |
| per_user_limit | 每人限领数量 |
| start_time | 开始时间 |
| end_time | 结束时间 |
| status | 活动状态 |
| version | 乐观锁版本号 |

### 11.3 领取记录表：coupon_record

存储用户实际领取到的优惠券。

主要字段：

| 字段 | 说明 |
|---|---|
| id | 记录编号 |
| user_id | 用户编号 |
| activity_id | 活动编号 |
| store_id | 门店编号 |
| coupon_name | 优惠券名称 |
| coupon_amount | 优惠金额 |
| threshold_amount | 使用门槛 |
| status | 使用状态 |
| receive_time | 领取时间 |
| use_time | 使用时间 |

唯一索引：

```sql
UNIQUE KEY uk_user_activity (user_id, activity_id)
```

### 11.4 秒杀订单表：coupon_seckill_order

存储抢券请求的处理状态。

主要字段：

| 字段 | 说明 |
|---|---|
| id | 秒杀订单编号 |
| user_id | 用户编号 |
| activity_id | 活动编号 |
| store_id | 门店编号 |
| status | 订单状态：排队中、成功、失败 |
| fail_reason | 失败原因 |
| request_id | 请求编号 |

唯一索引：

```sql
UNIQUE KEY uk_user_activity (user_id, activity_id)
UNIQUE KEY uk_request_id (request_id)
```

## 12. Redis Key 设计

| Key | 类型 | 说明 |
|---|---|---|
| `coupon:activity:{activityId}:info` | String | 活动基础信息 |
| `coupon:activity:{activityId}:stock` | String | 活动剩余库存 |
| `coupon:activity:{activityId}:users` | Set | 已领取用户集合 |
| `coupon:activity:{activityId}:soldout` | String | 售罄标记 |
| `coupon:seckill:result:{activityId}:{userId}` | String | 用户抢券结果 |
| `coupon:seckill:captcha:{captchaId}` | String | 验证码答案 |
| `coupon:seckill:token:{token}` | String | 秒杀令牌 |
| `coupon:limit:seckill:user:{userId}` | String | 用户抢券限流 |
| `coupon:limit:seckill:ip:{ip}` | String | IP 抢券限流 |

## 13. 项目结构

```text
restaurant-coupon-seckill
├── src
│   ├── main
│   │   ├── java/com/zym/restaurant/coupon
│   │   │   ├── controller
│   │   │   │   ├── admin              # 管理端接口
│   │   │   │   └── app                # 用户端接口
│   │   │   ├── service                # 业务层
│   │   │   ├── dal                    # 数据访问层
│   │   │   ├── mq                     # RabbitMQ 配置、生产者、消费者
│   │   │   ├── redis                  # Redis Key 与 Lua 相关代码
│   │   │   ├── framework              # 配置、工具、限流
│   │   │   ├── common                 # 通用返回、异常、分页
│   │   │   └── enums                  # 枚举
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-jmeter.yml
│   │       └── lua/coupon_seckill.lua
├── frontend/restaurant-coupon-seckill-ui  # Vue3 抢券演示页
├── sql                                    # 建表 SQL 与压测数据 SQL
├── docs                                   # 项目文档
├── jmeter                                 # JMeter 脚本
├── scripts                                # 压测运行脚本
├── tools                                  # JMeter 结果分析脚本
└── README.md
```

## 14. 环境准备

### 14.1 基础环境

| 环境 | 版本建议 |
|---|---|
| JDK | 17 |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| RabbitMQ | 3.x / 4.x management |
| Node.js | 18+ |
| npm | 9+ |
| JMeter | 5.6+ |

### 14.2 启动 RabbitMQ

推荐使用 Docker：

```bash
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:4-management
```

Windows PowerShell 一行命令：

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 -e RABBITMQ_DEFAULT_USER=guest -e RABBITMQ_DEFAULT_PASS=guest rabbitmq:4-management
```

RabbitMQ 管理后台：

```text
http://localhost:15672
```

账号密码：

```text
guest / guest
```

注意：

```text
5672  是 Spring Boot 连接 RabbitMQ 的端口
15672 是 RabbitMQ 浏览器管理后台端口
```

## 15. 后端启动步骤

### 15.1 创建数据库

```sql
CREATE DATABASE restaurant_coupon_seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 15.2 执行建表 SQL

```bash
mysql -uroot -proot restaurant_coupon_seckill < sql/01_schema.sql
```

或者在 Navicat / DataGrip 中直接执行：

```text
sql/01_schema.sql
```

### 15.3 修改配置

打开：

```text
src/main/resources/application.yml
```

修改 MySQL、Redis、RabbitMQ 配置：

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/restaurant_coupon_seckill?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: root

  data:
    redis:
      host: 127.0.0.1
      port: 6379
      database: 0

  rabbitmq:
    host: 127.0.0.1
    port: 5672
    username: guest
    password: guest
```

### 15.4 编译并启动

```bash
mvn clean compile
mvn spring-boot:run
```

启动成功后访问接口文档：

```text
http://localhost:8081/doc.html
```

健康检查：

```text
http://localhost:8081/actuator/health
```

## 16. 前端启动步骤

进入前端目录：

```bash
cd frontend/restaurant-coupon-seckill-ui
```

安装依赖：

```bash
npm install
```

启动前端：

```bash
npm run dev
```

访问：

```text
http://localhost:5173
```

前端默认通过 Vite 代理请求后端：

```text
/api -> http://127.0.0.1:8081
```

如果后端端口不是 8081，请修改：

```text
frontend/restaurant-coupon-seckill-ui/vite.config.ts
```

## 17. 演示流程

### 17.1 创建门店

```http
POST /admin/store/create
```

请求示例：

```json
{
  "name": "春熙路火锅店",
  "address": "成都市锦江区春熙路 88 号",
  "status": 0
}
```

### 17.2 创建优惠券活动

```http
POST /admin/coupon/activity/create
```

请求示例：

```json
{
  "storeId": 1,
  "title": "春熙路火锅店开业抢券活动",
  "couponName": "满100减30优惠券",
  "couponAmount": 30.00,
  "thresholdAmount": 100.00,
  "totalStock": 1000,
  "availableStock": 1000,
  "perUserLimit": 1,
  "startTime": "2026-07-01 12:00:00",
  "endTime": "2026-07-01 13:00:00"
}
```

### 17.3 发布活动并预热 Redis

```http
PUT /admin/coupon/activity/publish?id=1
```

如果需要手动预热：

```http
POST /admin/coupon/activity/preheat?id=1
```

查询 Redis 库存：

```http
GET /admin/coupon/activity/redis-stock?id=1
```

### 17.4 用户抢券

前端访问：

```text
http://localhost:5173
```

操作流程：

```text
选择模拟用户 ID，例如 20001
进入优惠券活动列表
点击“去抢券”
点击“获取验证码”
输入验证码答案
点击“创建秒杀令牌”
点击“立即抢券”
页面显示“排队中”
等待自动轮询结果
结果变成“领取成功”
进入“我的优惠券”查看领取记录
```

### 17.5 重复领取测试

同一个用户再次抢同一个活动，应该提示：

```text
请勿重复领取该优惠券
```

或者查询结果显示已经成功领取。

## 18. JMeter 压测

### 18.1 压测脚本

| 文件 | 用途 |
|---|---|
| `jmeter/01_core_seckill_token_disabled.jmx` | 核心抢券链路压测，关闭 token 校验 |
| `jmeter/02_full_flow_token_required.jmx` | 完整链路压测，包含验证码、令牌、抢券、结果查询 |

### 18.2 压测专用配置

启动后端时使用：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=jmeter
```

该配置会放宽限流，并关闭 token 校验，便于压测核心链路。

### 18.3 初始化压测数据

```bash
mysql -uroot -proot restaurant_coupon_seckill < sql/02_jmeter_reset_data.sql
```

清理 Redis：

```bash
redis-cli DEL coupon:activity:9001:info coupon:activity:9001:stock coupon:activity:9001:users coupon:activity:9001:soldout
```

预热库存：

```bash
curl -X POST "http://127.0.0.1:8081/admin/coupon/activity/preheat?id=9001"
```

### 18.4 执行核心链路压测

```bash
jmeter -n -t jmeter/01_core_seckill_token_disabled.jmx \
  -l reports/core_200.jtl \
  -e -o reports/html_core_200 \
  -Jthreads=200 \
  -JrampUp=10 \
  -Jloops=1
```

### 18.5 生成 Markdown 汇总

```bash
python tools/analyze_jmeter_result.py reports/core_200.jtl reports/core_200_summary.md
```

### 18.6 填写性能报告

压测完成后，把真实数据填写到：

```text
docs/jmeter/PERFORMANCE_REPORT.md
```

不要伪造 QPS、P95、P99，所有性能数据都应该来自本地真实压测结果。

## 19. 常见问题

### 19.1 Docker 能显示版本，但 docker ps 报错

如果出现：

```text
Failed to connect to the docker API
```

说明 Docker Desktop 后台没有启动。

解决方式：

```text
打开 Docker Desktop
等待 Docker Desktop is running
重新打开 PowerShell
执行 docker ps
```

### 19.2 RabbitMQ 容器名已存在

如果出现：

```text
Conflict. The container name "/rabbitmq" is already in use
```

说明 RabbitMQ 容器已经创建过。

直接启动即可：

```bash
docker start rabbitmq
```

### 19.3 前端请求后端报 ECONNREFUSED 127.0.0.1:8081

说明前端启动了，但后端 8081 没启动。

解决：

```text
先启动 MySQL、Redis、RabbitMQ
再启动后端 Spring Boot
确认 http://127.0.0.1:8081/doc.html 可以打开
最后启动前端
```

### 19.4 MyBatis Plus PaginationInnerInterceptor 编译报错

如果编译出现：

```text
找不到符号：PaginationInnerInterceptor
```

需要在 `pom.xml` 中加入：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>
```

MyBatis-Plus 3.5.9+ 之后分页插件需要单独引入该依赖。

### 19.5 活动库存未预热

如果抢券时报：

```text
活动库存未预热，请先预热
```

执行：

```http
POST /admin/coupon/activity/preheat?id=活动ID
```

或者重新发布活动。

### 19.6 RabbitMQ 后台打不开

确认容器是否运行：

```bash
docker ps
```

确认端口：

```text
15672 是管理后台端口
5672 是后端连接端口
```

浏览器访问：

```text
http://localhost:15672
```

## 20. 面试讲解重点

### 20.1 为什么不用数据库直接扣库存？

因为秒杀场景下并发请求很多，如果所有请求都直接访问数据库，数据库会承受很大压力。系统先把库存预热到 Redis，并用 Lua 原子扣库存，把大部分并发请求挡在 Redis 层，数据库只处理已经通过 Redis 校验的请求。

### 20.2 为什么用 Lua？

如果用多个 Redis 命令分别判断库存、判断重复领取、扣库存，在高并发下可能出现并发问题。Lua 脚本在 Redis 服务端一次性执行，可以保证库存判断、重复领取判断和库存扣减的原子性。

### 20.3 为什么还要数据库唯一索引？

Redis 是高并发入口层保护，数据库必须保留最终兜底。即使出现接口重试、MQ 重复消费、缓存异常，数据库唯一索引也能保证同一个用户不能重复领取同一个活动优惠券。

### 20.4 为什么用 RabbitMQ？

抢券高峰时，如果同步写数据库，接口响应会变慢，数据库压力也会增大。使用 RabbitMQ 后，抢券接口只负责快速校验和投递消息，真正的发券由消费者异步处理，实现削峰填谷。

### 20.5 MQ 重复消费怎么办？

消费者处理消息前会先查询秒杀订单状态。只有“排队中”的订单才会被处理。如果订单已经成功或失败，说明消息已经处理过，直接忽略。数据库唯一索引也会作为重复发券兜底。

### 20.6 Redis 扣库存成功但数据库失败怎么办？

如果 Redis Lua 预扣库存成功，但后续数据库落库失败，系统会执行补偿逻辑：移除用户领取标记、Redis 库存加回、更新售罄标记，并将订单结果设置为失败。

## 21. 简历写法

项目名称：

```text
餐饮门店优惠券秒杀系统
```

项目描述：

```text
基于 Spring Boot、Redis、Lua、RabbitMQ 和 MySQL 实现的餐饮门店优惠券秒杀系统，面向门店限量优惠券抢购场景，支持活动发布、库存预热、用户抢券、一人一券、异步发券、结果查询、接口限流和 JMeter 压测，解决高并发场景下的超卖、重复领取和数据库压力问题。
```

核心职责：

```text
1. 设计优惠券活动、领取记录、秒杀订单等核心表结构，通过 user_id + activity_id 唯一索引防止重复领取。
2. 实现 Redis 库存预热，将活动库存、售罄标记和已领取用户集合写入 Redis，减少高并发场景下的数据库访问压力。
3. 编写 Redis Lua 脚本，原子完成库存校验、重复领取校验、库存扣减和用户领取标记，防止超卖和一人多领。
4. 使用 RabbitMQ 实现异步发券，抢券接口只创建排队订单并投递消息，消费者异步扣减数据库库存、创建领取记录并更新抢券结果。
5. 实现验证码、秒杀令牌、用户限流和 IP 限流，防止活动未开始前提前构造请求和恶意刷接口。
6. 使用 JMeter 设计核心链路和完整防刷链路压测脚本，并通过订单、库存、领取记录进行数据一致性校验。
```

技术亮点：

```text
Redis + Lua 原子扣库存、RabbitMQ 异步削峰、唯一索引防重复领取、秒杀令牌防提前请求、限流防刷、JMeter 压测验证。
```

## 22. 后续优化方向

- 使用滑动窗口限流替代固定窗口限流；
- 增加 RabbitMQ 死信队列，处理多次消费失败消息；
- 增加消息重试次数与告警机制；
- 增加 Prometheus + Grafana 监控；
- 增加接口压测对比报告；
- 增加商家端活动后台页面；
- 增加优惠券核销功能；
- 增加用户登录和 JWT 鉴权；
- 使用 Docker Compose 一键启动 MySQL、Redis、RabbitMQ 与后端服务。

## 23. 运行顺序总结

完整运行顺序：

```text
启动 MySQL
启动 Redis
启动 RabbitMQ
执行 sql/01_schema.sql
启动后端 Spring Boot
启动前端 Vue3
创建门店
创建活动
发布活动
预热 Redis
前端抢券
查询结果
查看 RabbitMQ 队列
查看 MySQL 领取记录
```

## 24. 项目价值

本项目重点体现 Java 后端在高并发场景下的设计能力：

- 能够识别数据库在秒杀场景中的瓶颈；
- 能够使用 Redis 缓存和 Lua 脚本解决高并发库存扣减问题；
- 能够使用 MQ 对数据库写入进行异步削峰；
- 能够通过唯一索引、订单状态和补偿机制保证数据一致性；
- 能够设计验证码、秒杀令牌和限流机制防止恶意请求；
- 能够使用 JMeter 验证系统性能和数据正确性。

它和餐饮共享股东分红管理系统形成互补：一个体现 B 端企业业务闭环，一个体现 C 端高并发营销能力，适合作为 Java 后端实习简历项目组合。
