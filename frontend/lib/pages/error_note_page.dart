import 'dart:convert';
import 'package:flutter/material.dart';
import '../api/error_note_api.dart';

class ErrorNotePage extends StatefulWidget {
  const ErrorNotePage({super.key});

  @override
  State<ErrorNotePage> createState() => _ErrorNotePageState();
}

class _ErrorNotePageState extends State<ErrorNotePage> with SingleTickerProviderStateMixin {
  late TabController _tabCtrl;
  List<dynamic> _notes = [];
  int _currentPage = 1;
  int _totalPages = 1;
  bool _loading = false;
  bool? _masteredFilter;

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
    _tabCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadNotes() async {
    setState(() => _loading = true);
    try {
      final resp = await ErrorNoteApi.getList(
        page: _currentPage,
        mastered: _masteredFilter,
      );
      if (resp['code'] == 200) {
        final data = resp['data'];
        setState(() {
          _notes = data['records'] ?? [];
          _totalPages = data['pages'] ?? 1;
        });
      }
    } catch (_) {}
    setState(() => _loading = false);
  }

  Future<void> _toggleMastered(dynamic note) async {
    try {
      await ErrorNoteApi.markMastered(note['id'], !(note['mastered'] ?? false));
      _loadNotes();
    } catch (_) {}
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
