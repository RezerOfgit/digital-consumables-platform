package com.dcp.service;

import com.dcp.dto.ApplyDTO;
import com.dcp.entity.Material;
import com.dcp.entity.MaterialRecord;
import com.dcp.exception.BusinessException;
import com.dcp.mapper.MaterialMapper;
import com.dcp.mapper.RecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author Re-zero
 * @version 1.0
 */
@Service
public class RecordService {

    @Resource
    private RecordMapper recordMapper;

    // 注入耗材的 Mapper，因为我们需要查库存、扣库存
    @Resource
    private MaterialMapper materialMapper;

    /**
     * 提交领用申请 (核心业务)
     * @Transactional 注解保证了下面的操作要么全部成功，要么全部回滚！
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyMaterial(ApplyDTO applyDTO) {
        // 1. 查 (Check)：查询耗材是否存在，以及库存够不够
        // 这里需要你去 MaterialMapper.java 和 xml 里补一个 findById 方法（马上交给你办）
        Material material = materialMapper.findById(applyDTO.getMaterialId());

        if (material == null) {
            throw new BusinessException("该耗材不存在！");
        }
        if (material.getStock() < applyDTO.getQuantity()) {
            throw new BusinessException("库存不足！当前余量：" + material.getStock());
        }

        // 2. 扣 (Update)：扣减库存 (注意：传进去的是负数，代表扣减)
        // 这个方法你在 Day 2 已经写过了！
        int updatedRows = materialMapper.updateStock(applyDTO.getMaterialId(), -applyDTO.getQuantity());
        if (updatedRows == 0) {
            throw new BusinessException("扣减库存失败，请重试！");
        }

        // 3. 记 (Insert)：生成领用记录
        MaterialRecord materialRecord = new MaterialRecord();
        materialRecord.setMaterialId(applyDTO.getMaterialId());
        materialRecord.setApplicant(applyDTO.getApplicant());
        materialRecord.setQuantity(applyDTO.getQuantity());
        materialRecord.setRemark(applyDTO.getRemark());
        materialRecord.setStatus(1); // V1.0 简化流程，提交即发料成功(状态 1)

        int i = 1 / 0; // 人为制造运行时异常
        recordMapper.insert(materialRecord);
    }
}