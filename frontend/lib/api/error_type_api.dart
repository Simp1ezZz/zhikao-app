import 'client.dart';

class ErrorTypeApi {
  static Future<List<dynamic>> getList() async {
    final resp = await api.get('/error-types');
    if (resp.data['code'] == 200) {
      return resp.data['data'] ?? [];
    }
    return [];
  }
}
