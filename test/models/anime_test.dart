import 'package:flutter_test/flutter_test.dart';
import 'package:aniscroll_app/models/anime.dart';

void main() {
  group('Anime.fromJson', () {
    test('uses MAL English alternative title', () {
      final anime = Anime.fromJson({
        'id': 1,
        'title': 'Japanese default title',
        'alternative_titles': {'en': 'English Anime Title'},
        'mean': 8.5,
        'num_episodes': 12,
      });

      expect(anime.englishTitle, 'English Anime Title');
      expect(anime.hasEnglishTitle, isTrue);
    });

    test('does not display non-English default title when English is absent', () {
      final anime = Anime.fromJson({
        'id': 2,
        'title': 'Japanese default title',
        'alternative_titles': {'en': ''},
      });

      expect(anime.englishTitle, 'English title unavailable');
      expect(anime.hasEnglishTitle, isFalse);
    });
  });
}
