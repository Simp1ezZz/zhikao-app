import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'providers/auth_provider.dart';
import 'api/router.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final authProvider = AuthProvider();
  await authProvider.init();
  runApp(
    ChangeNotifierProvider.value(
      value: authProvider,
      child: const ZhikaoApp(),
    ),
  );
}

class ZhikaoApp extends StatelessWidget {
  const ZhikaoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: '智考',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      routerConfig: router,
    );
  }
}
