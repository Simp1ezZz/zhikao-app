package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.ErrorNote;
import com.zhikao.entity.Question;
import com.zhikao.mapper.ErrorNoteMapper;
import com.zhikao.mapper.QuestionMapper;
import com.zhikao.service.ErrorNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ErrorNoteServiceImpl extends ServiceImpl<ErrorNoteMapper, ErrorNote> implements ErrorNoteService {

    private final QuestionMapper questionMapper;

    @Override
    public IPage<Map<String, Object>> pageWithQuestion(Long userId, int page, int size, Boolean mastered) {
        LambdaQueryWrapper<ErrorNote> wrapper = new LambdaQueryWrapper<ErrorNote>()
                .eq(ErrorNote::getUserId, userId)
                .orderByDesc(ErrorNote::getCreatedAt);
        if (mastered != null) {
            wrapper.eq(ErrorNote::getMastered, mastered);
        }

        IPage<ErrorNote> notePage = baseMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (ErrorNote note : notePage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", note.getId());
            item.put("questionId", note.getQuestionId());
            item.put("errorTypes", note.getErrorTypes());
            item.put("note", note.getNote());
            item.put("reviewCount", note.getReviewCount());
            item.put("mastered", note.getMastered());
            item.put("createdAt", note.getCreatedAt());

            Question q = questionMapper.selectById(note.getQuestionId());
            if (q != null) {
                item.put("subject", q.getSubject());
                item.put("module", q.getModule());
                item.put("content", q.getContent());
                item.put("options", q.getOptions());
                item.put("answer", q.getAnswer());
                item.put("analysis", q.getAnalysis());
            }
            records.add(item);
        }

        Page<Map<String, Object>> result = new Page<>(notePage.getCurrent(), notePage.getSize(), notePage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public void updateNote(Long userId, Long id, String errorTypes, String note) {
        ErrorNote errorNote = getById(id);
        if (errorNote == null || !errorNote.getUserId().equals(userId)) {
            throw new RuntimeException("错题记录不存在");
        }
        errorNote.setErrorTypes(errorTypes);
        errorNote.setNote(note);
        updateById(errorNote);
    }

    @Override
    public void markMastered(Long userId, Long id, boolean mastered) {
        ErrorNote errorNote = getById(id);
        if (errorNote == null || !errorNote.getUserId().equals(userId)) {
            throw new RuntimeException("错题记录不存在");
        }
        errorNote.setMastered(mastered);
        updateById(errorNote);
    }

    @Override
    public Map<String, Object> getReviewList(Long userId, int count) {
        LambdaQueryWrapper<ErrorNote> wrapper = new LambdaQueryWrapper<ErrorNote>()
                .eq(ErrorNote::getUserId, userId)
                .eq(ErrorNote::getMastered, false)
                .and(w -> w.isNull(ErrorNote::getNextReviewAt)
                        .or().le(ErrorNote::getNextReviewAt, LocalDateTime.now()))
                .orderByAsc(ErrorNote::getReviewCount)
                .last("LIMIT " + count);

        List<ErrorNote> notes = baseMapper.selectList(wrapper);
        List<Long> questionIds = new ArrayList<>();
        for (ErrorNote n : notes) {
            questionIds.add(n.getQuestionId());
        }
        return Map.of("notes", notes, "total", notes.size());
    }

    @Override
    public void recordReview(Long userId, Long id) {
        ErrorNote errorNote = getById(id);
        if (errorNote == null || !errorNote.getUserId().equals(userId)) {
            throw new RuntimeException("错题记录不存在");
        }
        errorNote.setReviewCount(errorNote.getReviewCount() + 1);
        int interval = Math.min(errorNote.getReviewCount() * 2, 14);
        errorNote.setNextReviewAt(LocalDateTime.now().plusDays(interval));
        updateById(errorNote);
    }
}
