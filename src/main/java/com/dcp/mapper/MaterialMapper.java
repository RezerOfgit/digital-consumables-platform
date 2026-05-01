package com.dcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dcp.entity.Material;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Re-zero
 * @version 1.0
 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {

    // MyBatis-Plus 已提供：insert(), selectById(), selectList(), updateById(), deleteById()

//    // 分页或全量查询
//    List<Material> findAll();
//
//    // 根据分类 ID 筛选耗材（这是一个典型的业务场景）
//    List<Material> findByCategoryId(@Param("categoryId") Long categoryId);
//
//    int insert(Material material);

//    int updateStock(@Param("id") Long id, @Param("num") Integer num);

//    Material findById(Long id);
}