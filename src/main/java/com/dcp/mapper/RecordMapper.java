package com.dcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dcp.entity.MaterialRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 领用记录 Mapper。
 * @author Re-zero
 * @version 1.0
 */
@Mapper
public interface RecordMapper extends BaseMapper<MaterialRecord> {
}
