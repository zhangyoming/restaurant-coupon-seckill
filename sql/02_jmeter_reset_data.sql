USE restaurant_coupon_seckill;

-- JMeter 压测专用数据。
-- 注意：每次重新压测前建议执行本脚本，避免 user_id + activity_id 唯一索引导致大量重复领取。

DELETE FROM coupon_record WHERE activity_id = 9001;
DELETE FROM coupon_seckill_order WHERE activity_id = 9001;
DELETE FROM coupon_activity WHERE id = 9001;
DELETE FROM restaurant_store WHERE id = 901;

INSERT INTO restaurant_store(id, name, address, phone, status)
VALUES (901, 'JMeter 压测门店', '成都市高新区压测路 1 号', '028-99999999', 0);

INSERT INTO coupon_activity(id, store_id, title, coupon_name, coupon_amount, threshold_amount,
                            total_stock, available_stock, per_user_limit, start_time, end_time, status)
VALUES
(9001, 901, 'JMeter 高并发抢券活动', '满100减30压测券', 30.00, 100.00,
 100000, 100000, 1, '2026-01-01 00:00:00', '2036-12-31 23:59:59', 1);

-- 压测前还需要清理 Redis 缓存并重新预热：
-- redis-cli DEL coupon:activity:9001:info coupon:activity:9001:stock coupon:activity:9001:users coupon:activity:9001:soldout
-- curl -X POST "http://127.0.0.1:8081/admin/coupon/activity/preheat?id=9001"
