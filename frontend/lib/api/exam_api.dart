import 'client.dart';

class ExamApi {
  static Future<Map<String, dynamic>> start({
    String? subject,
    int count = 40,
    int timeLimitMinutes = 60,
  }) async {
    final resp = await api.post('/exam/start', data: {
      'subject': subject ?? '',
      'count': count,
      'timeLimitMinutes': timeLimitMinutes,
    });
    return resp.data;
  }

  static Future<Map<String, dynamic>> submit({
    required int examId,
    required Map<String, String> answers,
  }) async {
    final resp = await api.post('/exam/submit', data: {
      'examId': examId,
      'answers': answers,
    });
    return resp.data;
  }
}
