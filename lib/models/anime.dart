class Anime {
  const Anime({
    required this.id,
    required this.englishTitle,
    required this.synopsis,
    required this.imageUrl,
    required this.score,
    required this.episodeCount,
  });

  final int id;
  final String englishTitle;
  final String synopsis;
  final String? imageUrl;
  final double? score;
  final int? episodeCount;

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

    return Anime(
      id: (json['id'] as num?)?.toInt() ?? 0,
      englishTitle: englishTitle,
      synopsis: json['synopsis']?.toString() ?? '',
      imageUrl: imageUrl,
      score: rawScore is num ? rawScore.toDouble() : null,
      episodeCount: rawEpisodes is num && rawEpisodes > 0
          ? rawEpisodes.toInt()
          : null,
    );
  }
}
