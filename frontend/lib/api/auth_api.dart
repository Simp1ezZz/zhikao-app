import 'client.dart';

class AuthApi {
  static Future<Map<String, dynamic>> login(String username, String password) async {
    final resp = await api.post('/auth/login', data: {
      'username': username,
      'password': password,
    });
    return resp.data;
  }

  static Future<Map<String, dynamic>> register(String username, String password) async {
    final resp = await api.post('/auth/register', data: {
      'username': username,
      'password': password,
    });
    return resp.data;
  }
}
