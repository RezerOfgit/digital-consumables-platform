package com.dcp.service;

import com.dcp.entity.Material;
import com.dcp.exception.BusinessException;
import com.dcp.mapper.MaterialMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Re-zero
 * @version 1.0
 */
@Service
public class MaterialService {
    @Resource
    private MaterialMapper materialMapper;

    public List<Material> listAll() {
        return materialMapper.selectList(null);
    }

    public void addMaterial(Material material) {
        if (material.getStock() < 0) {
            throw new BusinessException("初始库存不能为负数");
        }
        materialMapper.insert(material);
    }
}