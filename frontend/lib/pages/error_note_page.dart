import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import '../api/error_note_api.dart';
import '../api/error_type_api.dart';
import '../api/ai_api.dart';

class ErrorNotePage extends StatefulWidget {
  const ErrorNotePage({super.key});

  @override
  State<ErrorNotePage> createState() => _ErrorNotePageState();
}

class _ErrorNotePageState extends State<ErrorNotePage> with SingleTickerProviderStateMixin {
  late TabController _tabCtrl;
  List<dynamic> _notes = [];
  Map<int, String> _errorTypeMap = {};
  int _currentPage = 1;
  int _totalPages = 1;
  bool _loading = false;
  bool? _masteredFilter;
  Map<int, String?> _aiAnalysisMap = {};
  Set<int> _aiLoadingIds = {};
  Set<int> _aiStreamingIds = {};
  Map<int, String?> _aiErrorMap = {};
  Map<int, StreamSubscription<String>> _aiSubs = {};

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 3, vsync: this);
    _tabCtrl.addListener(() {
      if (!_tabCtrl.indexIsChanging) {
        _masteredFilter = [null, false, true][_tabCtrl.index];
        _currentPage = 1;
        _loadNotes();
      }
    });
    _loadNotes();
  }

  @override
  void dispose() {
    for (final sub in _aiSubs.values) {
      sub.cancel();
    }
    _aiSubs.clear();
    _tabCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadNotes() async {
    setState(() => _loading = true);
    try {
      final results = await Future.wait([
        ErrorNoteApi.getList(page: _currentPage, mastered: _masteredFilter),
        ErrorTypeApi.getList(),
      ]);
      final noteResp = results[0] as Map<String, dynamic>;
      final typeResp = results[1] as List<dynamic>;
      if (noteResp['code'] == 200) {
        final data = noteResp['data'];
        setState(() {
          _notes = data['records'] ?? [];
          _totalPages = data['pages'] ?? 1;
        });
      }
      _errorTypeMap = {
        for (var t in typeResp) t['id'] as int: t['name'] as String,
      };
    } catch (_) {}
    setState(() => _loading = false);
  }

  Widget _buildAiAnalysis(dynamic note) {
    final qid = note['questionId'] as int;
    final analysis = _aiAnalysisMap[qid] ?? '';
    final loading = _aiLoadingIds.contains(qid);
    final streaming = _aiStreamingIds.contains(qid);
    final error = _aiErrorMap[qid];

    if (loading && !streaming) {
      return const Padding(
        padding: EdgeInsets.only(top: 8),
        child: Row(
          children: [
            SizedBox(width: 16, height: 16,
              child: CircularProgressIndicator(strokeWidth: 2)),
            SizedBox(width: 8),
            Text('AI 解析中...', style: TextStyle(fontSize: 13, color: Colors.grey)),
          ],
        ),
      );
    }

    if (streaming || analysis.isNotEmpty) {
      return Container(
        width: double.infinity,
        margin: const EdgeInsets.only(top: 8),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.blue.shade50,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.blue.shade200),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('AI 解析', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
            const SizedBox(height: 4),
            if (error != null)
              Text(error, style: const TextStyle(fontSize: 13, color: Colors.red))
            else
              MarkdownBody(
                data: analysis,
                selectable: true,
                styleSheet: MarkdownStyleSheet(
                  p: const TextStyle(fontSize: 13, height: 1.5),
                  h1: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  h2: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                  h3: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
                  listBullet: const TextStyle(fontSize: 13),
                  code: TextStyle(
                    fontSize: 12,
                    backgroundColor: Colors.grey.shade200,
                    fontFamily: 'monospace',
                  ),
                  codeblockDecoration: BoxDecoration(
                    color: Colors.grey.shade100,
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
              ),
            if (streaming)
              const Padding(
                padding: EdgeInsets.only(top: 8),
                child: SizedBox(
                  width: 16, height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              ),
          ],
        ),
      );
    }

    return Padding(
      padding: const EdgeInsets.only(top: 4),
      child: TextButton.icon(
        onPressed: () => _loadAiAnalysis(note),
        icon: const Icon(Icons.auto_awesome, size: 18),
        label: const Text('AI 解析'),
        style: TextButton.styleFrom(padding: EdgeInsets.zero, minimumSize: Size.zero),
      ),
    );
  }

  Widget _buildErrorTags(dynamic note) {
    List<int> typeIds = [];
    try {
      if (note['errorTypes'] is String && note['errorTypes'].toString().isNotEmpty) {
        final decoded = jsonDecode(note['errorTypes']);
        if (decoded is List) typeIds = List<int>.from(decoded);
      }
    } catch (_) {}
    final noteText = note['note']?.toString() ?? '';
    if (typeIds.isEmpty && noteText.isEmpty) return const SizedBox.shrink();

    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(top: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.orange.shade50,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.orange.shade200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (typeIds.isNotEmpty) ...[
            Wrap(
              spacing: 8,
              runSpacing: 6,
              children: typeIds.map((id) {
                final name = _errorTypeMap[id] ?? '未知';
                return Chip(
                  label: Text(name, style: const TextStyle(fontSize: 12)),
                  backgroundColor: Colors.orange.shade100,
                  padding: EdgeInsets.zero,
                  materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                );
              }).toList(),
            ),
          ],
          if (noteText.isNotEmpty) ...[
            if (typeIds.isNotEmpty) const SizedBox(height: 6),
            Text(noteText, style: const TextStyle(fontSize: 13, color: Colors.black87)),
          ],
        ],
      ),
    );
  }

  Future<void> _loadAiAnalysis(dynamic note) async {
    final qid = note['questionId'] as int;
    setState(() {
      _aiLoadingIds.add(qid);
      _aiStreamingIds.add(qid);
      _aiAnalysisMap[qid] = '';
      _aiErrorMap.remove(qid);
    });
    _aiSubs[qid]?.cancel();
    _aiSubs[qid] = AiApi.getAnalysisStream(qid).listen(
      (chunk) {
        if (mounted) {
          setState(() {
            _aiAnalysisMap[qid] = (_aiAnalysisMap[qid] ?? '') + chunk;
          });
        }
      },
      onError: (e) {
        if (mounted) {
          setState(() {
            _aiErrorMap[qid] = e.toString();
            _aiStreamingIds.remove(qid);
            _aiLoadingIds.remove(qid);
          });
        }
      },
      onDone: () {
        if (mounted) {
          setState(() {
            _aiStreamingIds.remove(qid);
            _aiLoadingIds.remove(qid);
          });
        }
      },
    );
  }

  Future<void> _toggleMastered(dynamic note) async {
    try {
      await ErrorNoteApi.markMastered(note['id'], !(note['mastered'] ?? false));
      _loadNotes();
    } catch (_) {}
  }

  Future<void> _reviewNote(dynamic note) async {
    try {
      await ErrorNoteApi.recordReview(note['id']);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('已标记复习完成')),
        );
      }
      _loadNotes();
    } catch (_) {}
  }

  Future<void> _editNote(dynamic note) async {
    final types = await ErrorTypeApi.getList();
    if (!mounted) return;
    List<int> selected = [];
    try {
      if (note['errorTypes'] is String) {
        selected = List<int>.from(jsonDecode(note['errorTypes']));
      }
    } catch (_) {}
    String noteText = note['note'] ?? '';
    await showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: const Text('编辑错因'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('错因类型：', style: TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: types.map((t) {
                    final id = t['id'] as int;
                    final selected_ = selected.contains(id);
                    return FilterChip(
                      label: Text(t['name'] ?? ''),
                      selected: selected_,
                      onSelected: (v) {
                        setDialogState(() {
                          if (v) selected.add(id);
                          else selected.remove(id);
                        });
                      },
                    );
                  }).toList(),
                ),
                const SizedBox(height: 16),
                const Text('补充说明：', style: TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                TextField(
                  maxLines: 2,
                  controller: TextEditingController(text: noteText),
                  decoration: const InputDecoration(
                    hintText: '请输入备注',
                    border: OutlineInputBorder(),
                  ),
                  onChanged: (v) => noteText = v,
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('取消'),
            ),
            ElevatedButton(
              onPressed: () async {
                await ErrorNoteApi.updateNote(
                  note['id'],
                  jsonEncode(selected),
                  noteText,
                );
                if (ctx.mounted) Navigator.pop(ctx);
                _loadNotes();
              },
              child: const Text('保存'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        TabBar(
          controller: _tabCtrl,
          tabs: const [
            Tab(text: '全部'),
            Tab(text: '未掌握'),
            Tab(text: '已掌握'),
          ],
        ),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : _notes.isEmpty
                  ? const Center(child: Text('暂无错题'))
                  : ListView.builder(
                      itemCount: _notes.length,
                      itemBuilder: (context, index) {
                        final note = _notes[index];
                        Map<String, dynamic> options = {};
                        try {
                          if (note['options'] is String) {
                            options = jsonDecode(note['options']);
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
                                    Chip(label: Text(note['subject'] ?? '')),
                                    const SizedBox(width: 8),
                                    Chip(label: Text(note['module'] ?? '')),
                                    const Spacer(),
                                    Text('复习${note['reviewCount'] ?? 0}次',
                                        style: const TextStyle(fontSize: 12, color: Colors.grey)),
                                    const SizedBox(width: 8),
                                    IconButton(
                                      icon: const Icon(Icons.edit_note, color: Colors.blue),
                                      onPressed: () => _editNote(note),
                                      tooltip: '编辑错因',
                                    ),
                                    IconButton(
                                      icon: const Icon(Icons.refresh, color: Colors.orange),
                                      onPressed: () => _reviewNote(note),
                                      tooltip: '复习此题',
                                    ),
                                    IconButton(
                                      icon: Icon(
                                        note['mastered'] == true ? Icons.check_circle : Icons.radio_button_unchecked,
                                        color: note['mastered'] == true ? Colors.green : Colors.grey,
                                      ),
                                      onPressed: () => _toggleMastered(note),
                                      tooltip: note['mastered'] == true ? '标记为未掌握' : '标记为已掌握',
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 8),
                                Text(note['content'] ?? '', style: const TextStyle(fontSize: 15)),
                                const SizedBox(height: 8),
                                ...options.entries.map((e) => Padding(
                                  padding: const EdgeInsets.only(bottom: 4),
                                  child: Text('${e.key}. ${e.value}', style: const TextStyle(fontSize: 14)),
                                )),
                                if (note['answer'] != null) ...[
                                  const SizedBox(height: 8),
                                  Text('正确答案: ${note['answer']}',
                                      style: const TextStyle(color: Colors.green, fontWeight: FontWeight.bold)),
                                ],
                                if (note['analysis'] != null && note['analysis'].toString().isNotEmpty) ...[
                                  const SizedBox(height: 4),
                                  Text('解析: ${note['analysis']}',
                                      style: const TextStyle(color: Colors.grey, fontSize: 13)),
                                ],
                                _buildAiAnalysis(note),
                                _buildErrorTags(note),
                              ],
                            ),
                          ),
                        );
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
                  onPressed: _currentPage > 1 ? () { _currentPage--; _loadNotes(); } : null,
                  icon: const Icon(Icons.chevron_left),
                ),
                Text('$_currentPage / $_totalPages'),
                IconButton(
                  onPressed: _currentPage < _totalPages ? () { _currentPage++; _loadNotes(); } : null,
                  icon: const Icon(Icons.chevron_right),
                ),
              ],
            ),
          ),
      ],
    );
  }
}
