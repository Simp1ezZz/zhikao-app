import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../api/auth_api.dart';

class AuthProvider extends ChangeNotifier {
  String? _token;
  String? _username;
  bool _loading = false;

  bool get isLoggedIn => _token != null;
  String? get username => _username;
  bool get loading => _loading;

  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    _token = prefs.getString('token');
    _username = prefs.getString('username');
    notifyListeners();
  }

  Future<String?> login(String username, String password) async {
    _loading = true;
    notifyListeners();
    try {
      final resp = await AuthApi.login(username, password);
      if (resp['code'] == 200) {
        final data = resp['data'];
        _token = data['token'];
        _username = data['username'];
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString('token', _token!);
        await prefs.setString('username', _username!);
        notifyListeners();
        return null;
      }
      return resp['message'] ?? '登录失败';
    } catch (e) {
      return '网络错误: $e';
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<String?> register(String username, String password) async {
    _loading = true;
    notifyListeners();
    try {
      final resp = await AuthApi.register(username, password);
      if (resp['code'] == 200) {
        return null;
      }
      return resp['message'] ?? '注册失败';
    } catch (e) {
      return '网络错误: $e';
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<void> logout() async {
    _token = null;
    _username = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('token');
    await prefs.remove('username');
    notifyListeners();
  }
}
