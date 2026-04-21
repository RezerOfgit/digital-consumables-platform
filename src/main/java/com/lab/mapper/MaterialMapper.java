package com.lab.mapper;

import com.lab.entity.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Re-zero
 * @version 1.0
 */
@Mapper
public interface MaterialMapper {
    // 分页或全量查询
    List<Material> findAll();

    // 根据分类 ID 筛选耗材（这是一个典型的业务场景）
    List<Material> findByCategoryId(@Param("categoryId") Long categoryId);

    int insert(Material material);

    int updateStock(@Param("id") Long id, @Param("num") Integer num);
}