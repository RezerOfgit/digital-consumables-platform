package com.dcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dcp.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统操作日志 Mapper。
 * @author Re-zero
 * @version 1.0
 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
}