import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../api/question_api.dart';

class QuestionListPage extends StatefulWidget {
  const QuestionListPage({super.key});

  @override
  State<QuestionListPage> createState() => _QuestionListPageState();
}

class _QuestionListPageState extends State<QuestionListPage> {
  List<dynamic> _questions = [];
  List<dynamic> _subjects = [];
  String? _selectedSubject;
  String? _selectedModule;
  List<dynamic> _modules = [];
  int _currentPage = 1;
  int _totalPages = 1;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _loadSubjects();
    _loadQuestions();
  }

  Future<void> _loadSubjects() async {
    try {
      final resp = await QuestionApi.getSubjects();
      if (resp['code'] == 200) {
        setState(() => _subjects = resp['data'] ?? []);
      }
    } catch (_) {}
  }

  Future<void> _loadQuestions() async {
    setState(() => _loading = true);
    try {
      final resp = await QuestionApi.getQuestions(
        page: _currentPage,
        subject: _selectedSubject,
        module: _selectedModule,
      );
      if (resp['code'] == 200) {
        final data = resp['data'];
        setState(() {
          _questions = data['records'] ?? [];
          _totalPages = data['pages'] ?? 1;
        });
      }
    } catch (_) {}
    setState(() => _loading = false);
  }

  void _onSubjectChanged(String? value) {
    _selectedSubject = value;
    _selectedModule = null;
    if (value != null) {
      final subj = _subjects.firstWhere((s) => s['name'] == value, orElse: () => null);
      _modules = subj != null ? (subj['children'] ?? []) : [];
    } else {
      _modules = [];
    }
    _currentPage = 1;
    _loadQuestions();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: DropdownButtonFormField<String>(
                  initialValue: _selectedSubject,
                  decoration: const InputDecoration(
                    labelText: '科目',
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                  items: [
                    const DropdownMenuItem(value: null, child: Text('全部科目')),
                    ..._subjects.map((s) => DropdownMenuItem(
                      value: s['name'] as String,
                      child: Text(s['name'] as String),
                    )),
                  ],
                  onChanged: _onSubjectChanged,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: DropdownButtonFormField<String>(
                  initialValue: _selectedModule,
                  decoration: const InputDecoration(
                    labelText: '模块',
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                  items: [
                    const DropdownMenuItem(value: null, child: Text('全部模块')),
                    ..._modules.map((m) => DropdownMenuItem(
                      value: m['name'] as String,
                      child: Text(m['name'] as String),
                    )),
                  ],
                  onChanged: (v) {
                    _selectedModule = v;
                    _currentPage = 1;
                    _loadQuestions();
                  },
                ),
              ),
              const SizedBox(width: 12),
              ElevatedButton.icon(
                onPressed: () {
                  if (_selectedSubject != null || _selectedModule != null) {
                    context.go('/practice?subject=${_selectedSubject ?? ""}&module=${_selectedModule ?? ""}');
                  } else {
                    context.go('/practice');
                  }
                },
                icon: const Icon(Icons.play_arrow),
                label: const Text('开始刷题'),
              ),
            ],
          ),
        ),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : _questions.isEmpty
                  ? const Center(child: Text('暂无题目'))
                  : ListView.builder(
                      itemCount: _questions.length,
                      itemBuilder: (context, index) {
                        final q = _questions[index];
                        return _QuestionCard(question: q);
                      },
                    ),
        ),
        if (_totalPages > 1)
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                IconButton(
                  onPressed: _currentPage > 1
                      ? () { _currentPage--; _loadQuestions(); }
                      : null,
                  icon: const Icon(Icons.chevron_left),
                ),
                Text('$_currentPage / $_totalPages'),
                IconButton(
                  onPressed: _currentPage < _totalPages
                      ? () { _currentPage++; _loadQuestions(); }
                      : null,
                  icon: const Icon(Icons.chevron_right),
                ),
              ],
            ),
          ),
      ],
    );
  }
}

class _QuestionCard extends StatelessWidget {
  final dynamic question;
  const _QuestionCard({required this.question});

  @override
  Widget build(BuildContext context) {
    Map<String, dynamic> options = {};
    try {
      if (question['options'] is String) {
        options = jsonDecode(question['options']);
      }
    } catch (_) {}

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Chip(label: Text(question['subject'] ?? '')),
                const SizedBox(width: 8),
                Chip(label: Text(question['module'] ?? '')),
                const SizedBox(width: 8),
                Text('难度: ${'★' * (question['difficulty'] ?? 3)}',
                    style: const TextStyle(color: Colors.orange, fontSize: 12)),
              ],
            ),
            const SizedBox(height: 8),
            Text(question['content'] ?? '', style: const TextStyle(fontSize: 15)),
            const SizedBox(height: 8),
            ...options.entries.map((e) => Padding(
              padding: const EdgeInsets.only(bottom: 4),
              child: Text('${e.key}. ${e.value}', style: const TextStyle(fontSize: 14)),
            )),
          ],
        ),
      ),
    );
  }
}

