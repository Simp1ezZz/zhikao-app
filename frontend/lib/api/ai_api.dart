import 'dart:async';
import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'client.dart';

class AiApi {
  static final Dio _aiDio = Dio(BaseOptions(
    baseUrl: api.options.baseUrl,
    connectTimeout: const Duration(seconds: 60),
    receiveTimeout: const Duration(seconds: 120),
    headers: {'Content-Type': 'application/json'},
  ));

  // ========== 非流式（兼容） ==========

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

  // ========== 流式 SSE ==========

  static Stream<String> getAnalysisStream(int questionId) async* {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');
    final baseUrl = api.options.baseUrl;

    final request = http.Request(
      'GET',
      Uri.parse('$baseUrl/ai/analysis/$questionId/stream'),
    );
    request.headers['Accept'] = 'text/event-stream';
    if (token != null) {
      request.headers['Authorization'] = 'Bearer $token';
    }

    final client = http.Client();
    try {
      final response = await client.send(request);

      if (response.statusCode != 200) {
        final body = await response.stream.bytesToString();
        throw Exception('AI服务错误: ${response.statusCode} - $body');
      }

      final buffer = StringBuffer();
      await for (final chunk in response.stream.transform(utf8.decoder)) {
        buffer.write(chunk);

        while (true) {
          final raw = buffer.toString();
          final idx = raw.indexOf('\n\n');
          if (idx == -1) break;

          final event = raw.substring(0, idx);
          buffer.clear();
          if (raw.length > idx + 2) {
            buffer.write(raw.substring(idx + 2));
          }

          for (final line in event.split('\n')) {
            if (line.startsWith('data: ')) {
              final data = line.substring(6);
              if (data == '[DONE]') return;
              if (data.startsWith('[ERROR]')) {
                throw Exception(data.substring(7).trim());
              }
              yield data;
            }
          }
        }
      }
    } finally {
      client.close();
    }
  }

  static Stream<String> chatStream(Map<String, dynamic> body) async* {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');
    final baseUrl = api.options.baseUrl;

    final request = http.Request(
      'POST',
      Uri.parse('$baseUrl/ai/chat/stream'),
    );
    request.headers['Content-Type'] = 'application/json';
    request.headers['Accept'] = 'text/event-stream';
    if (token != null) {
      request.headers['Authorization'] = 'Bearer $token';
    }
    request.body = jsonEncode(body);

    final client = http.Client();
    try {
      final response = await client.send(request);

      if (response.statusCode != 200) {
        final bodyStr = await response.stream.bytesToString();
        throw Exception('AI服务错误: ${response.statusCode} - $bodyStr');
      }

      final buffer = StringBuffer();
      await for (final chunk in response.stream.transform(utf8.decoder)) {
        buffer.write(chunk);

        while (true) {
          final raw = buffer.toString();
          final idx = raw.indexOf('\n\n');
          if (idx == -1) break;

          final event = raw.substring(0, idx);
          buffer.clear();
          if (raw.length > idx + 2) {
            buffer.write(raw.substring(idx + 2));
          }

          for (final line in event.split('\n')) {
            if (line.startsWith('data: ')) {
              final data = line.substring(6);
              if (data == '[DONE]') return;
              if (data.startsWith('[ERROR]')) {
                throw Exception(data.substring(7).trim());
              }
              yield data;
            }
          }
        }
      }
    } finally {
      client.close();
    }
  }
}
