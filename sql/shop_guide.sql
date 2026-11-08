-- MySQL dump 10.13  Distrib 8.0.34, for Win64 (x86_64)
--
-- Host: localhost    Database: shop_guide
-- ------------------------------------------------------
-- Server version	8.0.34

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `shop_guide`
--

/*!40000 DROP DATABASE IF EXISTS `shop_guide`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `shop_guide` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `shop_guide`;

--
-- Table structure for table `cart`
--

DROP TABLE IF EXISTS `cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '所属用户ID',
  `product_id` int NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) NOT NULL COMMENT '商品名称快照',
  `price` decimal(10,2) NOT NULL COMMENT '商品单价快照',
  `image` varchar(500) DEFAULT NULL COMMENT '商品图片快照',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '购买数量',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0未支付 1已支付',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart`
--

LOCK TABLES `cart` WRITE;
/*!40000 ALTER TABLE `cart` DISABLE KEYS */;
INSERT INTO `cart` VALUES (30,2,22,'游戏鼠标 无线',189.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793805105211_5c9329d0c41b41eca44bdcb52e91bd8d.webp',1,1,'2026-11-05 15:44:12','2026-11-05 15:44:21'),(31,2,23,'笔记本电脑支架',89.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793805151161_1e6026cf5bd44e98a26c4aaa318f0e4e.webp',1,1,'2026-11-05 15:44:13','2026-11-05 15:44:21'),(32,2,23,'笔记本电脑支架',89.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793805151161_1e6026cf5bd44e98a26c4aaa318f0e4e.webp',1,1,'2026-11-05 15:44:27','2026-11-05 15:44:32'),(37,2,19,'保温杯 500ml',59.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793804961248_73201621dec3470db9af8ad18f6d4623.webp',1,1,'2026-11-05 15:58:02','2026-11-05 15:58:12'),(38,2,15,'便携折叠小风扇',49.90,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793804788955_7ca70967002a42129678af45d4839538.jpg',1,1,'2026-11-05 15:58:04','2026-11-05 15:58:12');
/*!40000 ALTER TABLE `cart` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '分类主键',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `sort` int DEFAULT '0' COMMENT '排序号（越小越靠前）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` VALUES (1,'生活用品',1,'2026-11-05 14:52:09','2026-11-05 14:52:09'),(2,'电子产品',2,'2026-11-05 14:52:09','2026-11-05 14:52:09');
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product`
--

DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '商品名称',
  `price` decimal(10,2) NOT NULL COMMENT '商品价格',
  `image` varchar(500) DEFAULT NULL COMMENT '商品图片URL（阿里云OSS）',
  `category_id` int DEFAULT NULL COMMENT '所属分类ID（category.id）',
  `status` tinyint DEFAULT '1' COMMENT '状态：1上架 0下架',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product`
--

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` VALUES (13,'联想Y7000',7000.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793804499169_86f3bcbed4d341c8a2938edbe11ba7ee.webp',2,1,'2026-11-04 23:02:46','2026-11-04 23:03:22'),(14,'智能蓝牙耳机 Pro',299.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793804720317_514a310e8d2b427e9208906ebc4a0219.webp',2,1,'2026-11-04 23:05:22','2026-11-05 15:23:11'),(15,'便携折叠小风扇',49.90,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793804788955_7ca70967002a42129678af45d4839538.jpg',1,1,'2026-11-04 23:06:30','2026-11-04 23:24:29'),(16,'机械键盘 87键',399.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793804838062_f4c9f63fe3de48b8b2f1cb90748f82c7.webp',2,1,'2026-11-04 23:07:21','2026-11-04 23:07:21'),(17,'无线充电宝 2万毫安',129.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793804890359_27fcdc7ecec9418ba30e1113d35987b0.jpg',2,1,'2026-11-04 23:08:11','2026-11-04 23:08:11'),(18,'智能手环 8代',249.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793804925587_e348c09771ba4b7f9202b5913e9e86d6.webp',2,1,'2026-11-04 23:08:47','2026-11-04 23:08:47'),(19,'保温杯 500ml',59.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793804961248_73201621dec3470db9af8ad18f6d4623.webp',1,1,'2026-11-04 23:09:22','2026-11-04 23:09:22'),(20,'电动牙刷 声波款',199.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793805025232_3be03c1dd9eb439cbb7b8a33dc44f372.webp',2,1,'2026-11-04 23:10:26','2026-11-04 23:10:26'),(21,'桌面加湿器',79.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793805061987_f3e046b572a74eec955ed3dc7073ab69.jpg',1,1,'2026-11-04 23:11:04','2026-11-04 23:11:04'),(22,'游戏鼠标 无线',189.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793805105211_5c9329d0c41b41eca44bdcb52e91bd8d.webp',2,1,'2026-11-04 23:11:46','2026-11-05 15:06:28'),(23,'笔记本电脑支架',89.00,'https://projectww.oss-cn-beijing.aliyuncs.com/product/1793805151161_1e6026cf5bd44e98a26c4aaa318f0e4e.webp',1,1,'2026-11-04 23:12:32','2026-11-04 23:12:32');
/*!40000 ALTER TABLE `product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '账号',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `money` decimal(10,2) NOT NULL DEFAULT '10000.00' COMMENT '账户余额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'admin','123456',10000.00),(2,'wuwei','123456',9891.10),(3,'tanghanman','123456',10000.00),(4,'aiyanlin','123456',10000.00);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'shop_guide'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-05 16:40:39
