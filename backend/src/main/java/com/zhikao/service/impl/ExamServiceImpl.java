package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.entity.ErrorNote;
import com.zhikao.entity.PracticeRecord;
import com.zhikao.entity.Question;
import com.zhikao.mapper.ErrorNoteMapper;
import com.zhikao.mapper.PracticeRecordMapper;
import com.zhikao.mapper.QuestionMapper;
import com.zhikao.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final QuestionMapper questionMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final ErrorNoteMapper errorNoteMapper;

    private static final Map<Long, Map<String, Object>> examStore = new ConcurrentHashMap<>();
    private static long examIdSeq = 1;

    @Override
    public Map<String, Object> startExam(Long userId, String subject, int count, int timeLimitMinutes) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (subject != null && !subject.isEmpty()) {
            wrapper.eq(Question::getSubject, subject);
        }
        List<Question> all = questionMapper.selectList(wrapper);
        Collections.shuffle(all);

        List<Question> selected = all.subList(0, Math.min(count, all.size()));
        List<Map<String, Object>> questions = selected.stream().map(q -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("subject", q.getSubject());
            m.put("module", q.getModule());
            m.put("content", q.getContent());
            m.put("options", q.getOptions());
            m.put("difficulty", q.getDifficulty());
            m.put("estimatedTime", q.getEstimatedTime());
            return m;
        }).collect(Collectors.toList());

        long examId;
        synchronized (ExamServiceImpl.class) {
            examId = examIdSeq++;
        }

        Map<String, Object> exam = new HashMap<>();
        exam.put("userId", userId);
        exam.put("questions", selected);
        exam.put("startTime", System.currentTimeMillis());
        examStore.put(examId, exam);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("examId", examId);
        result.put("questions", questions);
        result.put("total", questions.size());
        result.put("timeLimitMinutes", timeLimitMinutes);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> submitExam(Long userId, Long examId, Map<String, String> answers) {
        Map<String, Object> exam = examStore.remove(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在或已提交");
        }

        List<Question> questions = (List<Question>) exam.get("questions");
        int correct = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (Question q : questions) {
            String userAnswer = answers.getOrDefault(String.valueOf(q.getId()), "");
            boolean isCorrect = q.getAnswer().equalsIgnoreCase(userAnswer.trim());
            if (isCorrect) correct++;

            PracticeRecord record = new PracticeRecord();
            record.setUserId(userId);
            record.setQuestionId(q.getId());
            record.setUserAnswer(userAnswer);
            record.setIsCorrect(isCorrect);
            record.setTimeSpent(0);
            record.setMode("EXAM");
            practiceRecordMapper.insert(record);

            if (!isCorrect) {
                Long existing = errorNoteMapper.selectCount(
                        new LambdaQueryWrapper<ErrorNote>()
                                .eq(ErrorNote::getUserId, userId)
                                .eq(ErrorNote::getQuestionId, q.getId()));
                if (existing == 0) {
                    ErrorNote note = new ErrorNote();
                    note.setUserId(userId);
                    note.setQuestionId(q.getId());
                    note.setErrorTypes("[]");
                    note.setNote("");
                    note.setReviewCount(0);
                    note.setMastered(false);
                    errorNoteMapper.insert(note);
                }
            }

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("questionId", q.getId());
            detail.put("userAnswer", userAnswer);
            detail.put("correctAnswer", q.getAnswer());
            detail.put("correct", isCorrect);
            detail.put("analysis", q.getAnalysis());
            details.add(detail);
        }

        double score = questions.isEmpty() ? 0 : (double) correct / questions.size() * 100;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", questions.size());
        result.put("correct", correct);
        result.put("score", Math.round(score * 10) / 10.0);
        result.put("details", details);
        return result;
    }
}
