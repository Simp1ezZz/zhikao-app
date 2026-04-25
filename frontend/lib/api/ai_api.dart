import 'client.dart';

class AiApi {
  static Future<Map<String, dynamic>> getAnalysis(int questionId) async {
    final resp = await api.get('/ai/analysis/$questionId');
    return resp.data;
  }
}
