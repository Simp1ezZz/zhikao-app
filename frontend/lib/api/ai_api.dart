import 'dart:async';
import 'dart:convert';
import 'dart:html' as html;
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

  // ========== 流式 SSE (Web 真流式) ==========

  static Stream<String> getAnalysisStream(int questionId) {
    return _createSseStream(
      method: 'GET',
      url: '${api.options.baseUrl}/ai/analysis/$questionId/stream',
    );
  }

  static Stream<String> chatStream(Map<String, dynamic> body) {
    return _createSseStream(
      method: 'POST',
      url: '${api.options.baseUrl}/ai/chat/stream',
      body: jsonEncode(body),
    );
  }

  static Stream<String> _createSseStream({
    required String method,
    required String url,
    String? body,
  }) {
    final controller = StreamController<String>();

    SharedPreferences.getInstance().then((prefs) {
      final token = prefs.getString('token');
      final xhr = html.HttpRequest();
      xhr.open(method, url);
      xhr.setRequestHeader('Accept', 'text/event-stream');
      if (token != null) {
        xhr.setRequestHeader('Authorization', 'Bearer $token');
      }
      if (body != null) {
        xhr.setRequestHeader('Content-Type', 'application/json');
      }

      var buffer = '';
      var lastLength = 0;

      void parseBuffer() {
        while (true) {
          final idx = buffer.indexOf('\n\n');
          if (idx == -1) break;

          final event = buffer.substring(0, idx);
          buffer = buffer.substring(idx + 2);

          // SSE spec: multiple data: lines are joined with \n
          final dataLines = <String>[];
          for (final line in event.split('\n')) {
            if (line.startsWith('data: ')) {
              dataLines.add(line.substring(6));
            }
          }
          if (dataLines.isEmpty) continue;

          final data = dataLines.join('\n');
          if (data == '[DONE]') {
            if (!controller.isClosed) controller.close();
            return;
          }
          if (data.startsWith('[ERROR]')) {
            if (!controller.isClosed) {
              controller.addError(Exception(data.substring(7).trim()));
              controller.close();
            }
            return;
          }
          controller.add(data);
        }
      }

      xhr.onProgress.listen((_) {
        final text = xhr.responseText ?? '';
        if (text.length > lastLength) {
          buffer += text.substring(lastLength);
          lastLength = text.length;
          parseBuffer();
        }
      });

      xhr.onLoad.listen((_) {
        final text = xhr.responseText ?? '';
        if (text.length > lastLength) {
          buffer += text.substring(lastLength);
          lastLength = text.length;
        }
        parseBuffer();
        if (!controller.isClosed) controller.close();
      });

      xhr.onError.listen((_) {
        if (!controller.isClosed) {
          controller.addError(Exception('网络请求失败'));
          controller.close();
        }
      });

      xhr.onTimeout.listen((_) {
        if (!controller.isClosed) {
          controller.addError(Exception('请求超时'));
          controller.close();
        }
      });

      xhr.send(body);
    }).catchError((e) {
      controller.addError(e);
      controller.close();
    });

    return controller.stream;
  }
}
