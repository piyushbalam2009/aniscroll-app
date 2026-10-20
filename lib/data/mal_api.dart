import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/anime.dart';

class MalApi {
  MalApi({http.Client? client}) : _client = client ?? http.Client();

  static const _baseUrl = 'https://api.myanimelist.net/v2';
  static const _clientId = String.fromEnvironment('MAL_CLIENT_ID');

  final http.Client _client;

  static const _fields =
      'alternative_titles,main_picture,synopsis,mean,num_episodes,genres,status';

  Future<List<Anime>> getTopAnime({int limit = 20}) async {
    _requireClientId();
    final uri = Uri.parse('$_baseUrl/anime/ranking').replace(
      queryParameters: {
        'ranking_type': 'all',
        'limit': '$limit',
        'fields': _fields,
      },
    );
    return _getAnimeList(uri);
  }

  Future<List<Anime>> searchAnime(String query, {int limit = 20}) async {
    _requireClientId();
    final trimmed = query.trim();
    if (trimmed.isEmpty) return const [];

    final uri = Uri.parse('$_baseUrl/anime').replace(
      queryParameters: {
        'q': trimmed,
        'limit': '$limit',
        'fields': _fields,
      },
    );
    return _getAnimeList(uri);
  }

  Future<List<Anime>> _getAnimeList(Uri uri) async {
    final response = await _client.get(
      uri,
      headers: {'X-MAL-CLIENT-ID': _clientId},
    );

    if (response.statusCode == 401 || response.statusCode == 403) {
      throw const MalApiException(
        'MAL rejected the client ID. Check your MAL API configuration.',
      );
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw MalApiException(
        'MAL request failed (${response.statusCode}). Please try again.',
      );
    }

    final decoded = jsonDecode(response.body) as Map<String, dynamic>;
    final rows = decoded['data'] as List<dynamic>? ?? const [];
    return rows
        .whereType<Map<String, dynamic>>()
        .map((row) => row['node'])
        .whereType<Map<String, dynamic>>()
        .map(Anime.fromJson)
        .where((anime) => anime.id > 0 && anime.hasEnglishTitle)
        .toList(growable: false);
  }

  void _requireClientId() {
    if (_clientId.trim().isEmpty) {
      throw const MalApiException(
        'MAL_CLIENT_ID is missing. Run with '
        '--dart-define=MAL_CLIENT_ID=YOUR_MAL_CLIENT_ID.',
      );
    }
  }

  void dispose() => _client.close();
}

class MalApiException implements Exception {
  const MalApiException(this.message);
  final String message;

  @override
  String toString() => message;
}
