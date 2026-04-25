import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../pages/login_page.dart';
import '../pages/home_page.dart';
import '../pages/question_list_page.dart';
import '../pages/practice_page.dart';
import '../pages/exam_page.dart';

final router = GoRouter(
  initialLocation: '/',
  redirect: (context, state) {
    final auth = context.read<AuthProvider>();
    final isLogin = state.matchedLocation == '/login';

    if (!auth.isLoggedIn && !isLogin) return '/login';
    if (auth.isLoggedIn && isLogin) return '/';
    return null;
  },
  routes: [
    GoRoute(path: '/login', builder: (_, _) => const LoginPage()),
    GoRoute(path: '/', builder: (_, _) => const HomePage()),
    GoRoute(path: '/questions', builder: (_, _) => const Scaffold(body: QuestionListPage())),
    GoRoute(
      path: '/practice',
      builder: (_, state) => PracticePage(
        subject: state.uri.queryParameters['subject'],
        module: state.uri.queryParameters['module'],
      ),
    ),
    GoRoute(
      path: '/exam',
      builder: (_, state) => ExamPage(
        subject: state.uri.queryParameters['subject'],
      ),
    ),
  ],
);
