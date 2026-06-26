-- ============================================
-- 批量创建压测用户
-- 生成 1000 个测试用户 (perfuser0001 ~ perfuser1000)
-- 密码统一为 123456 (MD5: e10adc3949ba59abbe56e057f20f883e)
-- ============================================

USE seckill_db;

-- 使用存储过程批量插入
DROP PROCEDURE IF EXISTS batch_insert_users;

DELIMITER //
CREATE PROCEDURE batch_insert_users(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE username VARCHAR(64);
    WHILE i <= total DO
        SET username = CONCAT('perfuser', LPAD(i, 4, '0'));
        INSERT IGNORE INTO seckill_user (username, password, phone, email, status)
        VALUES (username, 'e10adc3949ba59abbe56e057f20f883e',
                CONCAT('138', LPAD(i, 8, '0')),
                CONCAT(username, '@test.com'),
                1);
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

-- 执行: 创建 1000 个压测用户
CALL batch_insert_users(1000);

-- 验证
SELECT COUNT(*) AS total_users FROM seckill_user;
SELECT id, username FROM seckill_user WHERE username LIKE 'perfuser%' LIMIT 5;

DROP PROCEDURE IF EXISTS batch_insert_users;
