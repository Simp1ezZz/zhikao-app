import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'client.dart';

class AiApi {
  static final Dio _aiDio = Dio(BaseOptions(
    baseUrl: api.options.baseUrl,
    connectTimeout: const Duration(seconds: 60),
    receiveTimeout: const Duration(seconds: 120),
    headers: {'Content-Type': 'application/json'},
  ));

  static Future<Map<String, dynamic>> getAnalysis(int questionId) async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');
    final resp = await _aiDio.get(
      '/ai/analysis/$questionId',
      options: Options(
        headers: token != null ? {'Authorization': 'Bearer $token'} : null,
      ),
    );
    return resp.data;
  }
}
