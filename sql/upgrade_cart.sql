-- ============================================
-- 智能电商导购平台 升级脚本（购物车 + 余额）
-- 适用于已执行过 shop_guide.sql 的数据库，可重复执行
-- ============================================
USE shop_guide;

-- 1. 用户表新增余额字段（若不存在则添加）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'shop_guide' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'money');
SET @sql = IF(@col_exists = 0,
              'ALTER TABLE `user` ADD COLUMN `money` DECIMAL(10,2) NOT NULL DEFAULT 10000.00 COMMENT ''账户余额'' AFTER `password`',
              'SELECT ''money 字段已存在'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 给已有用户补一个初始余额（新用户默认10000）
UPDATE `user` SET `money` = 10000.00 WHERE `money` IS NULL;

-- 2. 购物车表
CREATE TABLE IF NOT EXISTS `cart` (
  `id`           INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `user_id`      INT NOT NULL COMMENT '所属用户ID',
  `product_id`   INT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称快照',
  `price`        DECIMAL(10,2) NOT NULL COMMENT '商品单价快照',
  `image`        VARCHAR(500) DEFAULT NULL COMMENT '商品图片快照',
  `quantity`     INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `status`       TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0未支付 1已支付',
  `create_time`  DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_time`  DATETIME DEFAULT NULL COMMENT '更新时间',
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';
