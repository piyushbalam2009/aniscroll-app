class Anime {
  const Anime({
    required this.id,
    required this.englishTitle,
    required this.synopsis,
    required this.imageUrl,
    required this.score,
    required this.episodeCount,
    this.genres = const [],
    this.startYear,
    this.season,
    this.format,
  });

  final int id;
  final String englishTitle;
  final String synopsis;
  final String? imageUrl;
  final double? score;
  final int? episodeCount;
  final List<String> genres;
  final int? startYear;
  final String? season;
  final String? format;

  bool get hasEnglishTitle => englishTitle != 'English title unavailable';

  factory Anime.fromJson(Map<String, dynamic> json) {
    final alternatives = json['alternative_titles'];
    final english = alternatives is Map<String, dynamic>
        ? alternatives['en']?.toString().trim()
        : null;
    final englishTitle = english != null && english.isNotEmpty
        ? english
        : 'English title unavailable';

    final picture = json['main_picture'];
    final imageUrl = picture is Map<String, dynamic>
        ? (picture['large'] ?? picture['medium'])?.toString()
        : null;

    final rawScore = json['mean'];
    final rawEpisodes = json['num_episodes'];
    final rawGenres = json['genres'];
    final genres = rawGenres is List
        ? rawGenres.whereType<Map<String, dynamic>>()
            .map((genre) => genre['name']?.toString() ?? '')
            .where((name) => name.isNotEmpty).toList(growable: false)
        : const <String>[];
    final rawSeason = json['start_season'];
    final season = rawSeason is Map<String, dynamic>
        ? rawSeason['season']?.toString()
        : null;
    final seasonYear = rawSeason is Map<String, dynamic>
        ? (rawSeason['year'] as num?)?.toInt()
        : null;
    final rawStartDate = json['start_date']?.toString();
    final startYear = seasonYear ?? (rawStartDate != null && rawStartDate.length >= 4
        ? int.tryParse(rawStartDate.substring(0, 4)) : null);

    return Anime(
      id: (json['id'] as num?)?.toInt() ?? 0,
      englishTitle: englishTitle,
      synopsis: json['synopsis']?.toString() ?? '',
      imageUrl: imageUrl,
      score: rawScore is num ? rawScore.toDouble() : null,
      episodeCount: rawEpisodes is num && rawEpisodes > 0
          ? rawEpisodes.toInt()
          : null,
      genres: genres,
      startYear: startYear,
      season: season,
      format: json['media_type']?.toString(),
    );
  }
}
