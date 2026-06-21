-- ============================================
-- Seckill System Database Initialization Script
-- ============================================

-- Create database
CREATE DATABASE IF NOT EXISTS seckill_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE seckill_db;

-- ============================================
-- Table: seckill_goods (秒杀商品表)
-- ============================================
DROP TABLE IF EXISTS seckill_goods;
CREATE TABLE seckill_goods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    goods_name VARCHAR(128) NOT NULL COMMENT '商品名称',
    goods_desc VARCHAR(512) DEFAULT '' COMMENT '商品描述',
    goods_picture VARCHAR(256) DEFAULT '' COMMENT '商品图片',
    original_price DECIMAL(10,2) NOT NULL COMMENT '原价',
    seckill_price DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    start_time DATETIME NOT NULL COMMENT '秒杀开始时间',
    end_time DATETIME NOT NULL COMMENT '秒杀结束时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-未发布, 1-即将开始, 2-进行中, 3-已结束',
    total_stock INT NOT NULL DEFAULT 0 COMMENT '总库存(用于统计)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';

-- ============================================
-- Table: seckill_order (秒杀订单表)
-- ============================================
DROP TABLE IF EXISTS seckill_order;
CREATE TABLE seckill_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    goods_name VARCHAR(128) NOT NULL COMMENT '商品名称(冗余)',
    goods_price DECIMAL(10,2) NOT NULL COMMENT '秒杀价格(冗余)',
    quantity INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态: 0-待支付, 1-已支付, 2-已取消, 3-已超时, 4-创建中',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
    expire_time DATETIME NOT NULL COMMENT '订单过期时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_goods_id (goods_id),
    INDEX idx_status (status),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';

-- ============================================
-- Table: seckill_user (用户表 - 简化版)
-- ============================================
DROP TABLE IF EXISTS seckill_user;
CREATE TABLE seckill_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码(MD5)',
    phone VARCHAR(20) DEFAULT '' COMMENT '手机号',
    email VARCHAR(128) DEFAULT '' COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- Sample Data
-- ============================================

-- Insert test user (password: 123456)
INSERT INTO seckill_user (username, password, phone, email) VALUES
('test001', 'e10adc3949ba59abbe56e057f20f883e', '13800138000', 'test001@seckill.com'),
('test002', 'e10adc3949ba59abbe56e057f20f883e', '13800138001', 'test002@seckill.com'),
('admin', 'e10adc3949ba59abbe56e057f20f883e', '13900139000', 'admin@seckill.com');

-- Insert test seckill goods
INSERT INTO seckill_goods (goods_name, goods_desc, goods_picture, original_price, seckill_price, stock, total_stock, start_time, end_time, status) VALUES
('iPhone 15 Pro Max 256GB', '苹果旗舰手机，A17 Pro芯片', 'https://example.com/iphone15.jpg', 9999.00, 7999.00, 100, 100, DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 3 HOUR), 1),
('小米14 Ultra', '小米旗舰手机，骁龙8 Gen3', 'https://example.com/xiaomi14.jpg', 6999.00, 4999.00, 200, 200, DATE_ADD(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 1),
('华为Mate 60 Pro', '华为旗舰手机，麒麟9000S', 'https://example.com/mate60.jpg', 7999.00, 5999.00, 150, 150, NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), 2),
('戴森V15吸尘器', '戴森旗舰吸尘器', 'https://example.com/dyson.jpg', 5999.00, 3999.00, 50, 50, NOW(), DATE_ADD(NOW(), INTERVAL 1 HOUR), 2),
('Switch OLED游戏机', '任天堂Switch OLED版', 'https://example.com/switch.jpg', 2599.00, 1999.00, 80, 80, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 0);
