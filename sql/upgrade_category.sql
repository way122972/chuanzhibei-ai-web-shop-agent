-- ============================================================
-- 商品分类功能升级脚本（可重复执行）
--   1. 新建 category 商品分类表（生活用品 / 电子产品）
--   2. product 表新增 category_id 字段（关联 category.id）
--   3. 按商品名称给已有商品归类
-- ============================================================
USE `shop_guide`;

-- 1. 商品分类表
CREATE TABLE IF NOT EXISTS `category` (
  `id`          INT AUTO_INCREMENT PRIMARY KEY COMMENT '分类主键',
  `name`        VARCHAR(50) NOT NULL UNIQUE COMMENT '分类名称',
  `sort`        INT DEFAULT 0 COMMENT '排序号（越小越靠前）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '商品分类表';

-- 2. 初始分类：生活用品(1) / 电子产品(2)
INSERT INTO `category` (`name`, `sort`) VALUES ('生活用品', 1)
    ON DUPLICATE KEY UPDATE `sort` = VALUES(`sort`);
INSERT INTO `category` (`name`, `sort`) VALUES ('电子产品', 2)
    ON DUPLICATE KEY UPDATE `sort` = VALUES(`sort`);

-- 3. product 表新增 category_id（存在则跳过，保证脚本可重复执行）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'category_id');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `product` ADD COLUMN `category_id` INT NULL COMMENT ''所属分类ID（category.id）'' AFTER `image`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 已有商品归类：电子产品 / 生活用品
UPDATE `product` SET `category_id` = (SELECT id FROM `category` WHERE name = '电子产品')
WHERE `name` IN ('联想Y7000', '智能蓝牙耳机 Pro', '机械键盘 87键', '无线充电宝 2万毫安',
                 '智能手环 8代', '电动牙刷 声波款', '游戏鼠标 无线');

UPDATE `product` SET `category_id` = (SELECT id FROM `category` WHERE name = '生活用品')
WHERE `name` IN ('保温杯 500ml', '桌面加湿器', '便携折叠小风扇', '笔记本电脑支架');

-- 5. 兜底：仍未归类的商品归入生活用品
UPDATE `product` SET `category_id` = (SELECT id FROM `category` WHERE name = '生活用品')
WHERE `category_id` IS NULL;

-- 6. 分类索引
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND INDEX_NAME = 'idx_category_id');
SET @ddl2 := IF(@idx_exists = 0, 'ALTER TABLE `product` ADD INDEX `idx_category_id` (`category_id`)', 'SELECT 1');
PREPARE stmt2 FROM @ddl2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 7. 校验
SELECT c.id, c.name, COUNT(p.id) AS product_count
FROM `category` c LEFT JOIN `product` p ON p.category_id = c.id
GROUP BY c.id, c.name ORDER BY c.sort;
