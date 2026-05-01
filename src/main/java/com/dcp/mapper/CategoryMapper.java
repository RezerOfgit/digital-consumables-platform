package com.dcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dcp.entity.Category;
import com.dcp.entity.Material;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author Re-zero
 * @version 1.0
 */
@Mapper // 告诉 Spring：这是一个 MyBatis 的映射接口
public interface CategoryMapper extends BaseMapper<Category> {
//    // 查询所有分类
//    List<Category> findAll();

//    // 根据 ID 查询
//    Category findById(Long id);

//    // 新增分类
//    int insert(Category category);

//    // 删除
//    int deleteById(Long id);
}