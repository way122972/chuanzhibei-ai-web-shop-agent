-- ============================================
-- 智能电商导购平台 数据库脚本
-- ============================================
CREATE DATABASE IF NOT EXISTS shop_guide DEFAULT CHARACTER SET utf8mb4;
USE shop_guide;

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`       INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '账号',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `money`    DECIMAL(10,2) NOT NULL DEFAULT 10000.00 COMMENT '账户余额'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 默认账号：admin / 123456（初始余额 10000）
INSERT INTO `user` (`username`, `password`, `money`) VALUES ('admin', '123456', 10000.00);

-- 商品分类表
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          INT AUTO_INCREMENT PRIMARY KEY COMMENT '分类主键',
  `name`        VARCHAR(50) NOT NULL UNIQUE COMMENT '分类名称',
  `sort`        INT DEFAULT 0 COMMENT '排序号（越小越靠前）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

INSERT INTO `category` (`name`, `sort`) VALUES ('生活用品', 1), ('电子产品', 2);

-- 商品表（category_id 关联 category.id）
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id`          INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `name`        VARCHAR(100) NOT NULL COMMENT '商品名称',
  `price`       DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
  `image`       VARCHAR(500)  DEFAULT NULL COMMENT '商品图片URL（阿里云OSS）',
  `category_id` INT           DEFAULT NULL COMMENT '所属分类ID（category.id）',
  `status`      TINYINT       DEFAULT 1 COMMENT '状态：1上架 0下架',
  `create_time` DATETIME      DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME      DEFAULT NULL COMMENT '更新时间',
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 示例商品数据（图片为示例占位图，新增商品后会替换为OSS真实地址）
INSERT INTO `product` (`name`, `price`, `image`, `category_id`, `status`, `create_time`, `update_time`) VALUES
('智能蓝牙耳机 Pro', 299.00, NULL, 2, 1, NOW(), NOW()),
('便携折叠小风扇', 49.90, NULL, 1, 1, NOW(), NOW()),
('机械键盘 87键', 399.00, NULL, 2, 1, NOW(), NOW()),
('无线充电宝 2万毫安', 129.00, NULL, 2, 1, NOW(), NOW()),
('智能手环 8代', 249.00, NULL, 2, 1, NOW(), NOW()),
('保温杯 500ml', 59.00, NULL, 1, 1, NOW(), NOW()),
('电动牙刷 声波款', 199.00, NULL, 2, 1, NOW(), NOW()),
('桌面加湿器', 79.00, NULL, 1, 1, NOW(), NOW()),
('游戏鼠标 无线', 189.00, NULL, 2, 1, NOW(), NOW()),
('笔记本电脑支架', 89.00, NULL, 1, 1, NOW(), NOW());

-- 购物车表
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
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
