import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:frontend/providers/auth_provider.dart';
import 'package:frontend/pages/login_page.dart';

void main() {
  testWidgets('Login page renders correctly', (WidgetTester tester) async {
    await tester.pumpWidget(
      ChangeNotifierProvider(
        create: (_) => AuthProvider(),
        child: const MaterialApp(home: LoginPage()),
      ),
    );

    expect(find.text('智考'), findsOneWidget);
    expect(find.text('公考刷题助手'), findsOneWidget);
    expect(find.byType(TextFormField), findsNWidgets(2));
    expect(find.text('登录'), findsOneWidget);
    expect(find.text('没有账号？去注册'), findsOneWidget);
  });

  testWidgets('Toggle to register mode', (WidgetTester tester) async {
    await tester.pumpWidget(
      ChangeNotifierProvider(
        create: (_) => AuthProvider(),
        child: const MaterialApp(home: LoginPage()),
      ),
    );

    await tester.tap(find.text('没有账号？去注册'));
    await tester.pump();

    expect(find.text('注册'), findsOneWidget);
    expect(find.text('已有账号？去登录'), findsOneWidget);
  });
}
