-- =====================================================
-- 数字化耗材管控平台 (DCP) 数据库初始化脚本
-- 版本: V1.0
-- 包含: 用户、分类、耗材、领用记录、审计日志、库存流水
-- =====================================================

CREATE DATABASE IF NOT EXISTS `dcp` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `dcp`;

-- ==================== 基础数据表 ====================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '加密后的密码',
  `real_name` VARCHAR(50) COMMENT '真实姓名',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: USER-实验员 ADMIN-库管员',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 管理员 admin，实验员 test01，初始密码均为 123456
INSERT INTO `user` (`username`, `password`, `real_name`, `role`) VALUES
('admin', '$2a$10$qj5.QkR87oTJzY9nXz56nO0J32kLQFw9qKBxqiWiM2LY4SbHCYcbu', '系统管理员', 'ADMIN'),
('test01', '$2a$10$qj5.QkR87oTJzY9nXz56nO0J32kLQFw9qKBxqiWiM2LY4SbHCYcbu', '实验员-张三', 'USER');

-- 耗材分类表
CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `sort` INT DEFAULT 0 COMMENT '排序权重',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='耗材分类表';

INSERT INTO `category` (`id`, `name`, `sort`) VALUES
(1, '万级洁净室防护用品', 10),
(2, 'PI中试线专用原料', 20),
(3, '锂电池研发高危试剂', 30);

-- ==================== 核心业务表 ====================

-- 耗材库存主表
CREATE TABLE IF NOT EXISTS `material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id` BIGINT NOT NULL COMMENT '所属分类ID',
  `name` VARCHAR(100) NOT NULL COMMENT '耗材名称',
  `specification` VARCHAR(50) COMMENT '规格型号',
  `unit` VARCHAR(20) NOT NULL COMMENT '计量单位',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '当前库存量',
  `danger_level` INT NOT NULL DEFAULT 0 COMMENT '危险等级: 0-普通 1-低危 2-高危 3-致命',
  `storage_condition` VARCHAR(100) COMMENT '存储条件',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_danger_level` (`danger_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='耗材库存主表';

INSERT INTO `material` (`category_id`, `name`, `specification`, `unit`, `stock`, `danger_level`, `storage_condition`) VALUES
(1, '丁腈无尘手套', '9寸-麻面-M码', '箱', 120, 0, '常温避光'),
(2, '均苯四甲酸二酐 (PMDA)', '纯度≥99.5%', 'kg', 50, 1, '密封防潮'),
(3, '氢氟酸 (HF)', '49%-500ml/瓶', '瓶', 30, 3, '专用防腐柜，双人双锁'),
(3, '硝酸 (HNO₃)', '65%-500ml/瓶', '瓶', 20, 3, '专用防腐柜，远离有机物'),
(2, '无水乙醇 (C₂H₅OH)', 'AR-500ml/瓶', '瓶', 50, 1, '密封远离火源');

-- 领用记录表
CREATE TABLE IF NOT EXISTS `record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `material_id` BIGINT NOT NULL COMMENT '领用的耗材ID',
  `applicant` VARCHAR(50) NOT NULL COMMENT '申请人姓名/工号',
  `quantity` INT NOT NULL COMMENT '申请领用数量',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待审批 1-已通过 2-已驳回 3-已归还',
  `remark` VARCHAR(255) COMMENT '用途说明/备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_material_id` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领用记录表';

-- ==================== 审计与流水表 ====================

-- 操作审计日志表
CREATE TABLE IF NOT EXISTS `sys_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作人',
  `module` VARCHAR(50) DEFAULT NULL COMMENT '操作模块',
  `action` VARCHAR(100) DEFAULT NULL COMMENT '操作动作',
  `params` TEXT COMMENT '方法入参（JSON 格式）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作审计日志表';

-- 库存变动流水表（V2.0 接入）
CREATE TABLE IF NOT EXISTS `stock_flow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `material_id` BIGINT NOT NULL COMMENT '耗材ID',
  `change_type` TINYINT NOT NULL COMMENT '变动类型: 1-入库, 2-领用出库, 3-报损出库, 4-归还',
  `change_quantity` INT NOT NULL COMMENT '变动数量',
  `before_stock` INT NOT NULL COMMENT '变动前库存',
  `after_stock` INT NOT NULL COMMENT '变动后库存',
  `operator_id` BIGINT COMMENT '操作人ID',
  `remark` VARCHAR(255) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_material_id` (`material_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动流水表（V2.0接入）';








