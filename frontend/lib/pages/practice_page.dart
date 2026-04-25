import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../api/practice_api.dart';
import '../api/question_api.dart';
import '../api/collection_api.dart';
import '../api/error_type_api.dart';
import '../api/error_note_api.dart';

class PracticePage extends StatefulWidget {
  final String? subject;
  final String? module;
  final bool isReviewMode;
  const PracticePage({super.key, this.subject, this.module, this.isReviewMode = false});

  @override
  State<PracticePage> createState() => _PracticePageState();
}

class _PracticePageState extends State<PracticePage> {
  List<int> _questionIds = [];
  int _currentIndex = 0;
  Map<String, dynamic>? _currentQuestion;
  String? _selectedAnswer;
  Map<String, dynamic>? _submitResult;
  bool _loading = true;
  bool _submitting = false;
  bool _collected = false;
  int _correctCount = 0;
  final Stopwatch _stopwatch = Stopwatch();
  Map<int, int> _errorNoteIdMap = {};

  // 错因标注
  List<dynamic> _errorTypes = [];
  final Set<int> _selectedErrorTypes = {};
  final TextEditingController _errorNoteCtrl = TextEditingController();
  int? _errorNoteId;

  @override
  void initState() {
    super.initState();
    _startPractice();
  }

  @override
  void dispose() {
    _errorNoteCtrl.dispose();
    super.dispose();
  }

  Future<void> _startPractice() async {
    if (widget.isReviewMode) {
      try {
        final resp = await ErrorNoteApi.getReviewList(count: 10);
        if (resp['code'] == 200) {
          final data = resp['data'] as Map<String, dynamic>;
          final notes = data['notes'] as List? ?? [];
          final ids = <int>[];
          _errorNoteIdMap = {};
          for (var n in notes) {
            final qid = n['questionId'] as int;
            ids.add(qid);
            _errorNoteIdMap[qid] = n['id'] as int;
          }
          setState(() => _questionIds = ids);
          if (ids.isNotEmpty) await _loadQuestion(ids[0]);
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('加载失败: $e')),
          );
        }
      }
    } else {
      try {
        final resp = await PracticeApi.start(
          subject: widget.subject,
          module: widget.module,
        );
        if (resp['code'] == 200) {
          final ids = (resp['data']['questionIds'] as List).cast<int>();
          setState(() => _questionIds = ids);
          if (ids.isNotEmpty) await _loadQuestion(ids[0]);
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('加载失败: $e')),
          );
        }
      }
    }
    setState(() => _loading = false);
  }

  Future<void> _loadQuestion(int id) async {
    setState(() {
      _selectedAnswer = null;
      _submitResult = null;
      _errorTypes = [];
      _selectedErrorTypes.clear();
      _errorNoteCtrl.clear();
      _errorNoteId = null;
    });
    try {
      final resp = await QuestionApi.getDetail(id);
      if (resp['code'] == 200) {
        setState(() => _currentQuestion = resp['data']);
        _stopwatch.reset();
        _stopwatch.start();
        _checkCollected(id);
      }
    } catch (_) {}
  }

  Future<void> _checkCollected(int id) async {
    try {
      final c = await CollectionApi.isCollected(id);
      if (mounted) setState(() => _collected = c);
    } catch (_) {}
  }

  Future<void> _submit() async {
    if (_selectedAnswer == null || _currentQuestion == null) return;
    _stopwatch.stop();
    setState(() => _submitting = true);
    try {
      final resp = await PracticeApi.submit(
        questionId: _currentQuestion!['id'],
        userAnswer: _selectedAnswer!,
        timeSpent: _stopwatch.elapsed.inSeconds,
      );
      if (resp['code'] == 200) {
        final data = resp['data'];
        if (data['correct'] == true) {
          _correctCount++;
        } else {
          _errorNoteId = data['errorNoteId'];
          _errorTypes = await ErrorTypeApi.getList();
        }
        if (widget.isReviewMode &&
            data['correct'] == true &&
            _errorNoteIdMap.containsKey(_currentQuestion!['id'])) {
          try {
            await ErrorNoteApi.recordReview(_errorNoteIdMap[_currentQuestion!['id']]!);
          } catch (_) {}
        }
        setState(() => _submitResult = data);
      }
    } catch (_) {}
    setState(() => _submitting = false);
  }

  Future<void> _saveErrorTag() async {
    if (_errorNoteId == null) return;
    try {
      await ErrorNoteApi.updateNote(
        _errorNoteId!,
        jsonEncode(_selectedErrorTypes.toList()),
        _errorNoteCtrl.text,
      );
    } catch (_) {}
  }

  void _next() {
    if (_currentIndex < _questionIds.length - 1) {
      _currentIndex++;
      _loadQuestion(_questionIds[_currentIndex]);
    } else {
      _showResult();
    }
  }

  void _showResult() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => AlertDialog(
        title: Text(widget.isReviewMode ? '复习完成' : '练习完成'),
        content: Text('共 ${_questionIds.length} 题，正确 $_correctCount 题\n'
            '正确率: ${(_questionIds.isEmpty ? 0 : _correctCount * 100 ~/ _questionIds.length)}%'),
        actions: [
          TextButton(
            onPressed: () { Navigator.pop(context); context.go('/'); },
            child: const Text('返回首页'),
          ),
        ],
      ),
    );
  }

  Future<void> _toggleCollect() async {
    if (_currentQuestion == null) return;
    try {
      await CollectionApi.toggle(_currentQuestion!['id']);
      setState(() => _collected = !_collected);
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return Scaffold(
        appBar: AppBar(title: Text(widget.isReviewMode ? '错题复习' : '刷题')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }
    if (_questionIds.isEmpty) {
      return Scaffold(
        appBar: AppBar(title: Text(widget.isReviewMode ? '错题复习' : '刷题')),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(widget.isReviewMode ? '今日没有需要复习的错题' : '没有找到题目',
                  style: const TextStyle(fontSize: 18)),
              const SizedBox(height: 16),
              ElevatedButton(onPressed: () => context.go('/'), child: const Text('返回')),
            ],
          ),
        ),
      );
    }

    final q = _currentQuestion;
    Map<String, dynamic> options = {};
    try {
      if (q?['options'] is String) options = jsonDecode(q!['options']);
    } catch (_) {}

    final bool answered = _submitResult != null;
    final bool wrong = answered && _submitResult!['correct'] != true;

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.isReviewMode
            ? '错题复习 第 ${_currentIndex + 1} / ${_questionIds.length} 题'
            : '第 ${_currentIndex + 1} / ${_questionIds.length} 题'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.go('/'),
        ),
        actions: [
          IconButton(
            icon: Icon(_collected ? Icons.star : Icons.star_border,
                color: _collected ? Colors.amber : null),
            onPressed: _toggleCollect,
          ),
        ],
      ),
      body: q == null
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 700),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(children: [
                        Chip(label: Text(q['subject'] ?? '')),
                        const SizedBox(width: 8),
                        Chip(label: Text(q['module'] ?? '')),
                      ]),
                      const SizedBox(height: 16),
                      Text(q['content'] ?? '',
                          style: const TextStyle(fontSize: 16, height: 1.6)),
                      const SizedBox(height: 20),
                      ...options.entries.map((e) => _OptionTile(
                        label: e.key,
                        text: e.value.toString(),
                        selected: _selectedAnswer == e.key,
                        correct: answered ? _submitResult!['answer'] : null,
                        onTap: answered ? null : () => setState(() => _selectedAnswer = e.key),
                      )),
                      const SizedBox(height: 24),
                      if (!answered)
                        SizedBox(
                          width: double.infinity,
                          height: 48,
                          child: ElevatedButton(
                            onPressed: _selectedAnswer != null && !_submitting ? _submit : null,
                            child: _submitting
                                ? const SizedBox(width: 20, height: 20,
                                    child: CircularProgressIndicator(strokeWidth: 2))
                                : const Text('提交答案'),
                          ),
                        ),
                      if (answered) ...[
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: _submitResult!['correct'] == true
                                ? Colors.green.shade50
                                : Colors.red.shade50,
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _submitResult!['correct'] == true ? '回答正确!' : '回答错误',
                                style: TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                  color: _submitResult!['correct'] == true
                                      ? Colors.green : Colors.red,
                                ),
                              ),
                              const SizedBox(height: 8),
                              Text('正确答案: ${_submitResult!['answer']}'),
                              if (_submitResult!['analysis'] != null) ...[
                                const SizedBox(height: 8),
                                Text('解析: ${_submitResult!['analysis']}'),
                              ],
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                        if (wrong) ...[
                          Container(
                            width: double.infinity,
                            padding: const EdgeInsets.all(16),
                            decoration: BoxDecoration(
                              color: Colors.orange.shade50,
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(color: Colors.orange.shade200),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    const Icon(Icons.label_outline, size: 18, color: Colors.orange),
                                    const SizedBox(width: 6),
                                    const Text('错因标注',
                                        style: TextStyle(fontWeight: FontWeight.bold)),
                                  ],
                                ),
                                const SizedBox(height: 12),
                                const Text('选择错因类型：', style: TextStyle(fontSize: 13)),
                                const SizedBox(height: 8),
                                Wrap(
                                  spacing: 8,
                                  runSpacing: 8,
                                  children: _errorTypes.map((t) {
                                    final id = t['id'] as int;
                                    final selected = _selectedErrorTypes.contains(id);
                                    return FilterChip(
                                      label: Text(t['name'] ?? '', style: const TextStyle(fontSize: 13)),
                                      selected: selected,
                                      onSelected: (v) {
                                        setState(() {
                                          if (v) _selectedErrorTypes.add(id);
                                          else _selectedErrorTypes.remove(id);
                                        });
                                        _saveErrorTag();
                                      },
                                    );
                                  }).toList(),
                                ),
                                const SizedBox(height: 12),
                                TextField(
                                  controller: _errorNoteCtrl,
                                  maxLines: 2,
                                  style: const TextStyle(fontSize: 13),
                                  decoration: const InputDecoration(
                                    hintText: '补充说明（可选）',
                                    isDense: true,
                                    contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                                    border: OutlineInputBorder(),
                                  ),
                                  onChanged: (_) => _saveErrorTag(),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 16),
                        ],
                        SizedBox(
                          width: double.infinity,
                          height: 48,
                          child: ElevatedButton(
                            onPressed: _next,
                            child: Text(_currentIndex < _questionIds.length - 1
                                ? '下一题' : '查看结果'),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ),
    );
  }
}

class _OptionTile extends StatelessWidget {
  final String label;
  final String text;
  final bool selected;
  final String? correct;
  final VoidCallback? onTap;

  const _OptionTile({
    required this.label,
    required this.text,
    required this.selected,
    this.correct,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    Color? bgColor;
    if (correct != null) {
      if (label == correct) {
        bgColor = Colors.green.shade100;
      } else if (selected) {
        bgColor = Colors.red.shade100;
      }
    } else if (selected) {
      bgColor = Colors.blue.shade50;
    }

    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          decoration: BoxDecoration(
            color: bgColor,
            border: Border.all(
              color: selected && correct == null ? Colors.blue : Colors.grey.shade300,
            ),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Text('$label. $text', style: const TextStyle(fontSize: 15)),
        ),
      ),
    );
  }
}
