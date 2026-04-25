import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../api/exam_api.dart';

class ExamPage extends StatefulWidget {
  final String? subject;
  const ExamPage({super.key, this.subject});

  @override
  State<ExamPage> createState() => _ExamPageState();
}

class _ExamPageState extends State<ExamPage> {
  List<dynamic> _questions = [];
  int _examId = 0;
  int _timeLimitMinutes = 60;
  int _remainingSeconds = 0;
  Timer? _timer;
  Map<String, String> _answers = {};
  int _currentIndex = 0;
  bool _loading = true;
  Map<String, dynamic>? _result;

  @override
  void initState() {
    super.initState();
    _startExam();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _startExam() async {
    try {
      final resp = await ExamApi.start(subject: widget.subject);
      if (resp['code'] == 200) {
        final data = resp['data'];
        setState(() {
          _examId = data['examId'];
          _questions = data['questions'] ?? [];
          _timeLimitMinutes = data['timeLimitMinutes'] ?? 60;
          _remainingSeconds = _timeLimitMinutes * 60;
        });
        _startTimer();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('加载失败: $e')));
      }
    }
    setState(() => _loading = false);
  }

  void _startTimer() {
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (_remainingSeconds <= 0) {
        _timer?.cancel();
        _submitExam();
        return;
      }
      setState(() => _remainingSeconds--);
    });
  }

  String get _timeDisplay {
    final m = _remainingSeconds ~/ 60;
    final s = _remainingSeconds % 60;
    return '${m.toString().padLeft(2, '0')}:${s.toString().padLeft(2, '0')}';
  }

  Future<void> _submitExam() async {
    _timer?.cancel();
    setState(() => _loading = true);
    try {
      final resp = await ExamApi.submit(examId: _examId, answers: _answers);
      if (resp['code'] == 200) {
        setState(() => _result = resp['data']);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('提交失败: $e')));
      }
    }
    setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return Scaffold(
        appBar: AppBar(title: const Text('模拟考试')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (_result != null) return _buildResult();
    if (_questions.isEmpty) {
      return Scaffold(
        appBar: AppBar(title: const Text('模拟考试')),
        body: Center(
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            const Text('没有找到题目'),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: () => context.go('/'), child: const Text('返回')),
          ]),
        ),
      );
    }

    final q = _questions[_currentIndex];
    Map<String, dynamic> options = {};
    try {
      if (q['options'] is String) options = jsonDecode(q['options']);
    } catch (_) {}
    final qId = q['id'].toString();

    return Scaffold(
      appBar: AppBar(
        title: Text('第 ${_currentIndex + 1} / ${_questions.length} 题'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => _showExitConfirm(),
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Center(
              child: Text(_timeDisplay,
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: _remainingSeconds < 300 ? Colors.red : null,
                  )),
            ),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 700),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(q['content'] ?? '', style: const TextStyle(fontSize: 16, height: 1.6)),
                const SizedBox(height: 20),
                ...options.entries.map((e) => Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: InkWell(
                    onTap: () => setState(() => _answers[qId] = e.key),
                    borderRadius: BorderRadius.circular(8),
                    child: Container(
                      width: double.infinity,
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                      decoration: BoxDecoration(
                        color: _answers[qId] == e.key ? Colors.blue.shade50 : null,
                        border: Border.all(
                          color: _answers[qId] == e.key ? Colors.blue : Colors.grey.shade300,
                        ),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text('${e.key}. ${e.value}', style: const TextStyle(fontSize: 15)),
                    ),
                  ),
                )),
                const SizedBox(height: 24),
                Row(
                  children: [
                    if (_currentIndex > 0)
                      Expanded(
                        child: OutlinedButton(
                          onPressed: () => setState(() => _currentIndex--),
                          child: const Text('上一题'),
                        ),
                      ),
                    if (_currentIndex > 0) const SizedBox(width: 12),
                    Expanded(
                      child: ElevatedButton(
                        onPressed: _currentIndex < _questions.length - 1
                            ? () => setState(() => _currentIndex++)
                            : _submitExam,
                        child: Text(_currentIndex < _questions.length - 1 ? '下一题' : '交卷'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                _buildAnswerSheet(),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildAnswerSheet() {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: List.generate(_questions.length, (i) {
        final qId = _questions[i]['id'].toString();
        final answered = _answers.containsKey(qId);
        return InkWell(
          onTap: () => setState(() => _currentIndex = i),
          child: Container(
            width: 36, height: 36,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: i == _currentIndex
                  ? Colors.blue
                  : answered ? Colors.green.shade100 : Colors.grey.shade200,
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text('${i + 1}',
                style: TextStyle(color: i == _currentIndex ? Colors.white : Colors.black)),
          ),
        );
      }),
    );
  }

  Widget _buildResult() {
    final details = (_result!['details'] as List?) ?? [];
    return Scaffold(
      appBar: AppBar(title: const Text('考试结果')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 700),
            child: Column(
              children: [
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(children: [
                      Text('${_result!['score']}分',
                          style: const TextStyle(fontSize: 48, fontWeight: FontWeight.bold, color: Colors.blue)),
                      const SizedBox(height: 8),
                      Text('${_result!['correct']} / ${_result!['total']} 题正确'),
                    ]),
                  ),
                ),
                const SizedBox(height: 16),
                ...details.asMap().entries.map((entry) {
                  final i = entry.key;
                  final d = entry.value;
                  return ListTile(
                    leading: Icon(
                      d['correct'] == true ? Icons.check_circle : Icons.cancel,
                      color: d['correct'] == true ? Colors.green : Colors.red,
                    ),
                    title: Text('第 ${i + 1} 题'),
                    subtitle: Text('你的答案: ${d['userAnswer']}  正确答案: ${d['correctAnswer']}'),
                  );
                }),
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: () => context.go('/'),
                  child: const Text('返回首页'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _showExitConfirm() {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('确认退出'),
        content: const Text('退出后本次考试将不会保存，确定退出吗？'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
          TextButton(
            onPressed: () { Navigator.pop(context); context.go('/'); },
            child: const Text('确定退出', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }
}
