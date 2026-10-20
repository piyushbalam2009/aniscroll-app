import 'package:flutter/material.dart';
import 'screens/home_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const AniscrollApp());
}

class AniscrollApp extends StatelessWidget {
  const AniscrollApp({super.key});

  @override
  Widget build(BuildContext context) {
    const accent = Color(0xFF8B5CF6);
    return MaterialApp(
      title: 'Aniscroll',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        colorScheme: ColorScheme.fromSeed(
          seedColor: accent,
          brightness: Brightness.dark,
          surface: const Color(0xFF111118),
        ),
        scaffoldBackgroundColor: const Color(0xFF09090F),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF09090F),
          foregroundColor: Colors.white,
          elevation: 0,
        ),
        useMaterial3: true,
      ),
      home: const HomeScreen(),
    );
  }
}
