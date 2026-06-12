CREATE DATABASE IF NOT EXISTS restaurant_coupon_seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE restaurant_coupon_seckill;

DROP TABLE IF EXISTS coupon_seckill_order;
DROP TABLE IF EXISTS coupon_record;
DROP TABLE IF EXISTS coupon_activity;
DROP TABLE IF EXISTS restaurant_store;

CREATE TABLE restaurant_store (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '门店编号',
    name VARCHAR(64) NOT NULL COMMENT '门店名称',
    address VARCHAR(255) DEFAULT NULL COMMENT '门店地址',
    phone VARCHAR(32) DEFAULT NULL COMMENT '门店电话',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0启用 1禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT NOT NULL DEFAULT b'0' COMMENT '是否删除',
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='餐饮门店';

CREATE TABLE coupon_activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '活动编号',
    store_id BIGINT NOT NULL COMMENT '门店编号',
    title VARCHAR(100) NOT NULL COMMENT '活动标题',
    coupon_name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    coupon_amount DECIMAL(10, 2) NOT NULL COMMENT '优惠金额',
    threshold_amount DECIMAL(10, 2) DEFAULT NULL COMMENT '使用门槛',
    total_stock INT NOT NULL COMMENT '总库存',
    available_stock INT NOT NULL COMMENT '剩余库存',
    per_user_limit INT NOT NULL DEFAULT 1 COMMENT '每人限领数量',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0草稿 1已发布 2已结束 3已下架',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT NOT NULL DEFAULT b'0' COMMENT '是否删除',
    KEY idx_store_id (store_id),
    KEY idx_status (status),
    KEY idx_start_end_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券秒杀活动';

CREATE TABLE coupon_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录编号',
    user_id BIGINT NOT NULL COMMENT '用户编号',
    activity_id BIGINT NOT NULL COMMENT '活动编号',
    store_id BIGINT NOT NULL COMMENT '门店编号',
    coupon_name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    coupon_amount DECIMAL(10, 2) NOT NULL COMMENT '优惠金额',
    threshold_amount DECIMAL(10, 2) DEFAULT NULL COMMENT '使用门槛',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0未使用 1已使用 2已过期',
    receive_time DATETIME NOT NULL COMMENT '领取时间',
    use_time DATETIME DEFAULT NULL COMMENT '使用时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT NOT NULL DEFAULT b'0' COMMENT '是否删除',
    UNIQUE KEY uk_user_activity (user_id, activity_id),
    KEY idx_user_id (user_id),
    KEY idx_activity_id (activity_id),
    KEY idx_store_id (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券领取记录';

CREATE TABLE coupon_seckill_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '秒杀订单编号',
    user_id BIGINT NOT NULL COMMENT '用户编号',
    activity_id BIGINT NOT NULL COMMENT '活动编号',
    store_id BIGINT NOT NULL COMMENT '门店编号',
    status TINYINT NOT NULL COMMENT '状态：0排队中 1成功 2失败',
    fail_reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    request_id VARCHAR(64) NOT NULL COMMENT '请求编号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT NOT NULL DEFAULT b'0' COMMENT '是否删除',
    UNIQUE KEY uk_request_id (request_id),
    UNIQUE KEY uk_user_activity (user_id, activity_id),
    KEY idx_user_id (user_id),
    KEY idx_activity_id (activity_id),
    KEY idx_store_id (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券秒杀请求订单';

INSERT INTO restaurant_store(id, name, address, phone, status)
VALUES
(1, '春熙路火锅店', '成都市锦江区春熙路 88 号', '028-10000001', 0),
(2, '天府广场烤肉店', '成都市青羊区天府广场 66 号', '028-10000002', 0);

INSERT INTO coupon_activity(id, store_id, title, coupon_name, coupon_amount, threshold_amount,
                            total_stock, available_stock, per_user_limit, start_time, end_time, status)
VALUES
(1001, 1, '春熙路火锅店开业抢券', '满100减30优惠券', 30.00, 100.00,
 100, 100, 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1),
(1002, 2, '天府广场烤肉店午市券', '满80减20优惠券', 20.00, 80.00,
 50, 50, 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1);
