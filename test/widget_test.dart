
import 'package:flutter_test/flutter_test.dart';
import 'package:aniscroll_app/main.dart';

void main() {
  testWidgets('Aniscroll app launches', (WidgetTester tester) async {
    await tester.pumpWidget(const AniscrollApp());
    await tester.pump();

    expect(find.text('AniScroll'), findsOneWidget);
  });
}
