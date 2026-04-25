import 'client.dart';

class PracticeApi {
  static Future<Map<String, dynamic>> start({
    String? subject,
    String? module,
    int count = 20,
  }) async {
    final resp = await api.post('/practice/start', data: {
      'subject': subject ?? '',
      'module': module ?? '',
      'count': count,
    });
    return resp.data;
  }

  static Future<Map<String, dynamic>> submit({
    required int questionId,
    required String userAnswer,
    int timeSpent = 0,
  }) async {
    final resp = await api.post('/practice/submit', data: {
      'questionId': questionId,
      'userAnswer': userAnswer,
      'timeSpent': timeSpent,
    });
    return resp.data;
  }
}
