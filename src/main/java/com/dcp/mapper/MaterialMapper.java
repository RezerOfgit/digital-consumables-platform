package com.dcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dcp.entity.Material;
import org.apache.ibatis.annotations.Mapper;

/**
 * 耗材信息 Mapper。
 * @author Re-zero
 * @version 1.0
 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {
}