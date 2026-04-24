package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.SubjectConfig;
import com.zhikao.mapper.SubjectConfigMapper;
import com.zhikao.service.SubjectConfigService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubjectConfigServiceImpl extends ServiceImpl<SubjectConfigMapper, SubjectConfig> implements SubjectConfigService {

    @Override
    public List<Map<String, Object>> getTree() {
        List<SubjectConfig> all = list(new LambdaQueryWrapper<SubjectConfig>()
                .eq(SubjectConfig::getEnabled, true)
                .orderByAsc(SubjectConfig::getSortOrder));

        Map<Long, List<SubjectConfig>> grouped = all.stream()
                .collect(Collectors.groupingBy(SubjectConfig::getParentId));

        return buildTree(grouped, 0L);
    }

    private List<Map<String, Object>> buildTree(Map<Long, List<SubjectConfig>> grouped, Long parentId) {
        List<SubjectConfig> children = grouped.getOrDefault(parentId, Collections.emptyList());
        List<Map<String, Object>> result = new ArrayList<>();
        for (SubjectConfig item : children) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", item.getId());
            node.put("name", item.getName());
            node.put("level", item.getLevel());
            node.put("sortOrder", item.getSortOrder());
            node.put("enabled", item.getEnabled());
            List<Map<String, Object>> sub = buildTree(grouped, item.getId());
            if (!sub.isEmpty()) {
                node.put("children", sub);
            }
            result.add(node);
        }
        return result;
    }
}
