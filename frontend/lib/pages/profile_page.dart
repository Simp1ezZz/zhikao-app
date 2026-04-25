import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../api/stats_api.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key});

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  Map<String, dynamic>? _stats;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadStats();
  }

  Future<void> _loadStats() async {
    try {
      final resp = await StatsApi.getOverview();
      if (resp['code'] == 200) {
        setState(() => _stats = resp['data']);
      }
    } catch (_) {}
    setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 600),
          child: Column(
            children: [
              const CircleAvatar(radius: 40, child: Icon(Icons.person, size: 40)),
              const SizedBox(height: 12),
              Text(auth.username ?? '用户',
                  style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 32),
              if (_loading)
                const CircularProgressIndicator()
              else if (_stats != null)
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('学习统计', style: Theme.of(context).textTheme.titleMedium),
                        const Divider(),
                        _StatRow('总练习题数', '${_stats!['totalPractice'] ?? 0}'),
                        _StatRow('正确题数', '${_stats!['correctCount'] ?? 0}'),
                        _StatRow('正确率', '${_stats!['accuracy'] ?? 0}%'),
                        _StatRow('错题数', '${_stats!['errorCount'] ?? 0}'),
                        _StatRow('已掌握', '${_stats!['masteredCount'] ?? 0}'),
                        _StatRow('收藏数', '${_stats!['collectionCount'] ?? 0}'),
                      ],
                    ),
                  ),
                ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: () async {
                    await auth.logout();
                    if (context.mounted) context.go('/login');
                  },
                  icon: const Icon(Icons.logout, color: Colors.red),
                  label: const Text('退出登录', style: TextStyle(color: Colors.red)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatRow extends StatelessWidget {
  final String label;
  final String value;
  const _StatRow(this.label, this.value);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(fontSize: 15)),
          Text(value, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }
}
