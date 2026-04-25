import 'client.dart';

class ErrorNoteApi {
  static Future<Map<String, dynamic>> getList({
    int page = 1,
    int size = 10,
    bool? mastered,
  }) async {
    final params = <String, dynamic>{'page': page, 'size': size};
    if (mastered != null) params['mastered'] = mastered;
    final resp = await api.get('/error-notes', queryParameters: params);
    return resp.data;
  }

  static Future<void> updateNote(int id, String errorTypes, String note) async {
    await api.put('/error-notes/$id', data: {
      'errorTypes': errorTypes,
      'note': note,
    });
  }

  static Future<void> markMastered(int id, bool mastered) async {
    await api.put('/error-notes/$id/mastered', data: {'mastered': mastered});
  }

  static Future<Map<String, dynamic>> getReviewList({int count = 10}) async {
    final resp = await api.get('/error-notes/review', queryParameters: {'count': count});
    return resp.data;
  }

  static Future<void> recordReview(int id) async {
    await api.post('/error-notes/$id/review');
  }
}
