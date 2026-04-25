import 'package:flutter/material.dart';
import '../api/stats_api.dart';

class ReportPage extends StatefulWidget {
  const ReportPage({super.key});

  @override
  State<ReportPage> createState() => _ReportPageState();
}

class _ReportPageState extends State<ReportPage> {
  Map<String, dynamic>? _overview;
  List<dynamic>? _trend;
  List<dynamic>? _subjectStats;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    try {
      final results = await Future.wait([
        StatsApi.getOverview(),
        StatsApi.getTrend(days: 7),
        StatsApi.getSubjectStats(),
      ]);
      setState(() {
        _overview = results[0]['data'];
        _trend = results[1]['data']?['trend'];
        _subjectStats = results[2]['data'];
      });
    } catch (_) {}
    setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 700),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('学习报告', style: Theme.of(context).textTheme.headlineSmall),
              const SizedBox(height: 16),
              if (_overview != null) _buildOverviewCard(),
              const SizedBox(height: 20),
              if (_trend != null) _buildTrendCard(),
              const SizedBox(height: 20),
              if (_subjectStats != null && _subjectStats!.isNotEmpty) _buildSubjectCard(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildOverviewCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('总览', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const Divider(),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _StatBox('总练习', '${_overview!['totalPractice'] ?? 0}', Colors.blue),
                _StatBox('正确率', '${_overview!['accuracy'] ?? 0}%', Colors.green),
                _StatBox('错题', '${_overview!['errorCount'] ?? 0}', Colors.red),
                _StatBox('已掌握', '${_overview!['masteredCount'] ?? 0}', Colors.orange),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTrendCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('近7天趋势', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const Divider(),
            SizedBox(
              height: 120,
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: _trend!.map((d) {
                  final total = (d['total'] ?? 0) as int;
                  final maxVal = _trend!.fold<int>(1, (m, e) {
                    final v = (e['total'] ?? 0) as int;
                    return v > m ? v : m;
                  });
                  final height = maxVal > 0 ? (total / maxVal * 80).toDouble() : 0.0;
                  final date = (d['date'] ?? '').toString();
                  final label = date.length >= 10 ? date.substring(5) : date;
                  return Expanded(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        Text('$total', style: const TextStyle(fontSize: 11)),
                        const SizedBox(height: 4),
                        Container(
                          height: height,
                          margin: const EdgeInsets.symmetric(horizontal: 4),
                          decoration: BoxDecoration(
                            color: Colors.blue.shade300,
                            borderRadius: BorderRadius.circular(4),
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(label, style: const TextStyle(fontSize: 10)),
                      ],
                    ),
                  );
                }).toList(),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSubjectCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('科目分析', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const Divider(),
            ..._subjectStats!.map((s) {
              final accuracy = (s['accuracy'] ?? 0).toDouble();
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 6),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(s['subject'] ?? '', style: const TextStyle(fontSize: 14)),
                        Text('${s['correct']}/${s['total']}  $accuracy%',
                            style: const TextStyle(fontSize: 13, color: Colors.grey)),
                      ],
                    ),
                    const SizedBox(height: 4),
                    LinearProgressIndicator(
                      value: accuracy / 100,
                      backgroundColor: Colors.grey.shade200,
                      color: accuracy >= 80 ? Colors.green : accuracy >= 60 ? Colors.orange : Colors.red,
                    ),
                  ],
                ),
              );
            }),
          ],
        ),
      ),
    );
  }
}

class _StatBox extends StatelessWidget {
  final String label;
  final String value;
  final Color color;
  const _StatBox(this.label, this.value, this.color);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(value, style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: color)),
        const SizedBox(height: 4),
        Text(label, style: const TextStyle(fontSize: 13, color: Colors.grey)),
      ],
    );
  }
}
