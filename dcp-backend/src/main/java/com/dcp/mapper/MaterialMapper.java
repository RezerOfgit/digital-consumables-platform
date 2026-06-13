package com.dcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dcp.entity.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 耗材信息 Mapper。
 * @author Re-zero
 * @version 1.0
 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {

    /**
     * 原子扣减库存（数据库行锁保证并发安全）
     * stock >= quantity 作为前置条件，不足时 updatedRows = 0
     */
    @Update("UPDATE material SET stock = stock - #{quantity}, version = version + 1, " +
            "update_time = NOW() WHERE id = #{id} AND stock >= #{quantity} AND is_deleted = 0")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);
}