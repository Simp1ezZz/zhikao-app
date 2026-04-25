package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.entity.ErrorNote;
import com.zhikao.entity.PracticeRecord;
import com.zhikao.entity.Question;
import com.zhikao.mapper.ErrorNoteMapper;
import com.zhikao.mapper.PracticeRecordMapper;
import com.zhikao.mapper.QuestionMapper;
import com.zhikao.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeServiceImpl implements PracticeService {

    private final QuestionMapper questionMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final ErrorNoteMapper errorNoteMapper;

    @Override
    public Map<String, Object> startPractice(Long userId, String subject, String module, int count) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (subject != null && !subject.isEmpty()) {
            wrapper.eq(Question::getSubject, subject);
        }
        if (module != null && !module.isEmpty()) {
            wrapper.eq(Question::getModule, module);
        }
        wrapper.select(Question::getId);
        List<Question> all = questionMapper.selectList(wrapper);

        List<Long> ids;
        if (all.size() <= count) {
            ids = all.stream().map(Question::getId).collect(Collectors.toList());
        } else {
            Collections.shuffle(all);
            ids = all.subList(0, count).stream().map(Question::getId).collect(Collectors.toList());
        }

        return Map.of("questionIds", ids, "total", ids.size());
    }

    @Override
    public Map<String, Object> submitAnswer(Long userId, Long questionId, String userAnswer, int timeSpent) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("题目不存在");
        }

        boolean correct = question.getAnswer().equalsIgnoreCase(userAnswer.trim());

        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setQuestionId(questionId);
        record.setUserAnswer(userAnswer);
        record.setIsCorrect(correct);
        record.setTimeSpent(timeSpent);
        record.setMode("PRACTICE");
        practiceRecordMapper.insert(record);

        Long errorNoteId = null;
        if (!correct) {
            ErrorNote existing = errorNoteMapper.selectOne(
                    new LambdaQueryWrapper<ErrorNote>()
                            .eq(ErrorNote::getUserId, userId)
                            .eq(ErrorNote::getQuestionId, questionId));
            if (existing == null) {
                ErrorNote note = new ErrorNote();
                note.setUserId(userId);
                note.setQuestionId(questionId);
                note.setErrorTypes("[]");
                note.setNote("");
                note.setReviewCount(0);
                note.setMastered(false);
                errorNoteMapper.insert(note);
                errorNoteId = note.getId();
            } else {
                errorNoteId = existing.getId();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("correct", correct);
        result.put("answer", question.getAnswer());
        result.put("analysis", question.getAnalysis());
        result.put("errorNoteId", errorNoteId);
        return result;
    }
}
