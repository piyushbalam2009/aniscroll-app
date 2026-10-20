import 'package:flutter/material.dart';

import '../models/anime.dart';
import 'player_screen.dart';

class AnimeDetailsScreen extends StatelessWidget {
  const AnimeDetailsScreen({super.key, required this.anime});

  final Anime anime;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(anime.englishTitle, maxLines: 1,
            overflow: TextOverflow.ellipsis),
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 320, maxHeight: 420),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(18),
                child: anime.imageUrl == null
                    ? const ColoredBox(
                        color: Color(0xFF20202B),
                        child: Center(child: Icon(Icons.movie_outlined, size: 56)),
                      )
                    : Image.network(
                        anime.imageUrl!,
                        fit: BoxFit.cover,
                        errorBuilder: (_, __, ___) => const ColoredBox(
                          color: Color(0xFF20202B),
                          child: Center(
                            child: Icon(Icons.movie_outlined, size: 56),
                          ),
                        ),
                      ),
              ),
            ),
          ),
          const SizedBox(height: 20),
          Text(
            anime.englishTitle,
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 10),
          Wrap(
            alignment: WrapAlignment.center,
            spacing: 16,
            children: [
              if (anime.score != null)
                Chip(label: Text('MAL ★ ${anime.score!.toStringAsFixed(1)}')),
              Chip(
                label: Text(anime.episodeCount == null
                    ? 'Episode count unavailable'
                    : '${anime.episodeCount} episodes'),
              ),
            ],
          ),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute<void>(
                builder: (_) => PlayerScreen(anime: anime),
              ),
            ),
            icon: const Icon(Icons.play_arrow_rounded),
            label: const Text('Play on ZokoAnime'),
          ),
          const SizedBox(height: 24),
          Text('Synopsis',
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.bold,
                  )),
          const SizedBox(height: 8),
          Text(anime.synopsis.isEmpty
              ? 'Synopsis unavailable from MyAnimeList.'
              : anime.synopsis),
        ],
      ),
    );
  }
}
