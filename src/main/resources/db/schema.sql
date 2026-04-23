-- =====================================================
-- 数字化耗材管控平台 (DCP) 数据库初始化脚本
-- 版本: V1.0
-- 包含: 用户、分类、耗材、库存流水、审计日志
-- =====================================================

-- 1. 建库
CREATE DATABASE IF NOT EXISTS `dcp` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


USE `dcp`;

-- 创建耗材分类表
CREATE TABLE `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `sort` INT DEFAULT 0 COMMENT '排序权重',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='耗材分类表';

-- 插入真实的分类数据
INSERT INTO `category` (`id`, `name`, `sort`) VALUES
(1, '万级洁净室防护用品', 10),
(2, 'PI中试线专用原料', 20),
(3, '锂电池研发高危试剂', 30);

SELECT * FROM CATEGORY C ;


USE `dcp`;

CREATE TABLE IF NOT EXISTS `material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id` BIGINT NOT NULL COMMENT '所属分类ID',
  `name` VARCHAR(100) NOT NULL COMMENT '耗材名称',
  `specification` VARCHAR(50) COMMENT '规格型号',
  `unit` VARCHAR(20) NOT NULL COMMENT '计量单位',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '当前库存量',
  `danger_level` INT NOT NULL DEFAULT 0 COMMENT '危险等级: 0-普通, 1-低危, 2-高危, 3-致命',
  `storage_condition` VARCHAR(100) COMMENT '存储条件',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='耗材详情表';

-- 预插入几条硬核数据，方便等下直接测试查询接口
INSERT INTO `material` (`category_id`, `name`, `specification`, `unit`, `stock`, `danger_level`, `storage_condition`) VALUES
(1, '丁腈无尘手套', '9寸-麻面-M码', '箱', 120, 0, '常温避光'),
(2, '均苯四甲酸二酐 (PMDA)', '纯度≥99.5%', 'kg', 50, 1, '密封防潮'),
(3, '氢氟酸 (HF)', '49%-500ml/瓶', '瓶', 30, 3, '专用防腐柜，双人双锁');














































