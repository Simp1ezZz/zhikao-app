import 'client.dart';

class StatsApi {
  static Future<Map<String, dynamic>> getOverview() async {
    final resp = await api.get('/stats/overview');
    return resp.data;
  }

  static Future<Map<String, dynamic>> getTrend({int days = 7}) async {
    final resp = await api.get('/stats/trend', queryParameters: {'days': days});
    return resp.data;
  }

  static Future<Map<String, dynamic>> getSubjectStats() async {
    final resp = await api.get('/stats/subjects');
    return resp.data;
  }
}
