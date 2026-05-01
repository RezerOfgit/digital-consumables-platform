package com.dcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dcp.entity.Material;
import com.dcp.entity.MaterialRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Re-zero
 * @version 1.0
 */
@Mapper
public interface RecordMapper extends BaseMapper<MaterialRecord> {
//    // 插入领用记录
//    int insert(MaterialRecord record);
}
