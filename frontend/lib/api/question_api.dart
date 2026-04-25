import 'client.dart';

class QuestionApi {
  static Future<Map<String, dynamic>> getQuestions({
    int page = 1,
    int size = 20,
    String? subject,
    String? module,
  }) async {
    final params = <String, dynamic>{'page': page, 'size': size};
    if (subject != null && subject.isNotEmpty) params['subject'] = subject;
    if (module != null && module.isNotEmpty) params['module'] = module;
    final resp = await api.get('/questions', queryParameters: params);
    return resp.data;
  }

  static Future<Map<String, dynamic>> getDetail(int id) async {
    final resp = await api.get('/questions/$id');
    return resp.data;
  }

  static Future<Map<String, dynamic>> getSubjects() async {
    final resp = await api.get('/questions/subjects');
    return resp.data;
  }
}
