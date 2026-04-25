import 'client.dart';

class CollectionApi {
  static Future<Map<String, dynamic>> toggle(int questionId, {String note = ''}) async {
    final resp = await api.post('/collections/toggle', data: {
      'questionId': questionId,
      'note': note,
    });
    return resp.data;
  }

  static Future<bool> isCollected(int questionId) async {
    final resp = await api.get('/collections/check/$questionId');
    return resp.data['data']['collected'] == true;
  }
}
