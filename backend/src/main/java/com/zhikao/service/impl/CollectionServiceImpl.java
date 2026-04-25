package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.Collection;
import com.zhikao.entity.Question;
import com.zhikao.mapper.CollectionMapper;
import com.zhikao.mapper.QuestionMapper;
import com.zhikao.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl extends ServiceImpl<CollectionMapper, Collection> implements CollectionService {

    private final QuestionMapper questionMapper;

    @Override
    public IPage<Map<String, Object>> pageWithQuestion(Long userId, int page, int size) {
        LambdaQueryWrapper<Collection> wrapper = new LambdaQueryWrapper<Collection>()
                .eq(Collection::getUserId, userId)
                .orderByDesc(Collection::getCreatedAt);

        IPage<Collection> colPage = baseMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Collection col : colPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", col.getId());
            item.put("questionId", col.getQuestionId());
            item.put("note", col.getNote());
            item.put("createdAt", col.getCreatedAt());

            Question q = questionMapper.selectById(col.getQuestionId());
            if (q != null) {
                item.put("subject", q.getSubject());
                item.put("module", q.getModule());
                item.put("content", q.getContent());
                item.put("options", q.getOptions());
            }
            records.add(item);
        }

        Page<Map<String, Object>> result = new Page<>(colPage.getCurrent(), colPage.getSize(), colPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public void toggle(Long userId, Long questionId, String note) {
        LambdaQueryWrapper<Collection> wrapper = new LambdaQueryWrapper<Collection>()
                .eq(Collection::getUserId, userId)
                .eq(Collection::getQuestionId, questionId);
        Collection existing = baseMapper.selectOne(wrapper);

        if (existing != null) {
            baseMapper.deleteById(existing.getId());
        } else {
            Collection col = new Collection();
            col.setUserId(userId);
            col.setQuestionId(questionId);
            col.setNote(note != null ? note : "");
            baseMapper.insert(col);
        }
    }

    @Override
    public boolean isCollected(Long userId, Long questionId) {
        return baseMapper.selectCount(new LambdaQueryWrapper<Collection>()
                .eq(Collection::getUserId, userId)
                .eq(Collection::getQuestionId, questionId)) > 0;
    }
}
