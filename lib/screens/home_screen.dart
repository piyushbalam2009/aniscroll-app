import 'package:flutter/material.dart';

import '../data/mal_api.dart';
import '../models/anime.dart';
import 'anime_details_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _api = MalApi();
  final _searchController = TextEditingController();
  List<Anime> _anime = const [];
  final Map<int, Anime> _watchlist = {};
  bool _loading = true;
  bool _searchMode = false;
  String? _error;
  int _destination = 0;
  String _filter = 'All';

  List<Anime> get _visibleAnime {
    final source = _destination == 1 ? _watchlist.values.toList() : _anime;
    switch (_filter) {
      case 'Top rated':
        return source.where((anime) => (anime.score ?? 0) >= 8).toList()
          ..sort((a, b) => (b.score ?? 0).compareTo(a.score ?? 0));
      case 'Short series':
        return source
            .where((anime) =>
                anime.episodeCount != null && anime.episodeCount! <= 24)
            .toList();
      default:
        return source;
    }
  }

  @override
  void initState() {
    super.initState();
    _loadTopAnime();
  }

  @override
  void dispose() {
    _api.dispose();
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadTopAnime() async {
    setState(() {
      _loading = true;
      _error = null;
      _searchMode = false;
      _destination = 0;
      _filter = 'All';
    });
    try {
      final results = await _api.getTopAnime();
      if (!mounted) return;
      setState(() {
        _anime = results;
        _loading = false;
      });
    } on Exception catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error.toString().replaceFirst('Exception: ', '');
        _loading = false;
      });
    }
  }

  Future<void> _search() async {
    final query = _searchController.text.trim();
    if (query.isEmpty) {
      await _loadTopAnime();
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
      _searchMode = true;
      _destination = 0;
      _filter = 'All';
    });
    try {
      final results = await _api.searchAnime(query);
      if (!mounted) return;
      setState(() {
        _anime = results;
        _loading = false;
      });
    } on Exception catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error.toString().replaceFirst('Exception: ', '');
        _loading = false;
      });
    }
  }

  void _toggleWatchlist(Anime anime) {
    final wasSaved = _watchlist.containsKey(anime.id);
    setState(() {
      if (wasSaved) {
        _watchlist.remove(anime.id);
      } else {
        _watchlist[anime.id] = anime;
      }
    });
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(wasSaved ? 'Removed from My List' : 'Added to My List'),
          duration: const Duration(seconds: 2),
          action: SnackBarAction(
            label: 'UNDO',
            onPressed: () {
              setState(() {
                if (wasSaved) {
                  _watchlist[anime.id] = anime;
                } else {
                  _watchlist.remove(anime.id);
                }
              });
            },
          ),
        ),
      );
  }

  void _openAnime(Anime anime) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => AnimeDetailsScreen(anime: anime),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isWatchlist = _destination == 1;
    final screenWidth = MediaQuery.sizeOf(context).width;
    final isTvLayout = screenWidth >= 1000;
    final pageHorizontalPadding = isTvLayout ? 36.0 : 16.0;
    return Scaffold(
      appBar: AppBar(
        titleSpacing: 20,
        title: Row(
          children: [
            Container(
              width: 34,
              height: 34,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFFB794F6), Color(0xFF7C3AED)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(11),
              ),
              child: const Icon(Icons.play_arrow_rounded,
                  color: Colors.white, size: 25),
            ),
            const SizedBox(width: 10),
            const Text(
              'AniScroll',
              style: TextStyle(fontWeight: FontWeight.w900, letterSpacing: -0.6),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Refresh anime',
            onPressed: _loading ? null : _loadTopAnime,
            icon: const Icon(Icons.refresh_rounded),
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: EdgeInsets.fromLTRB(pageHorizontalPadding, isTvLayout ? 20 : 8, pageHorizontalPadding, isTvLayout ? 20 : 14),
              child: TextField(
                controller: _searchController,
                textInputAction: TextInputAction.search,
                onSubmitted: (_) => _search(),
                onChanged: (_) => setState(() {}),
                decoration: InputDecoration(
                  hintText: 'Search anime in English',
                  prefixIcon: const Icon(Icons.search_rounded),
                  suffixIcon: _searchController.text.isEmpty
                      ? IconButton(
                          tooltip: 'Search',
                          onPressed: _search,
                          icon: const Icon(Icons.arrow_forward_rounded),
                        )
                      : IconButton(
                          tooltip: 'Clear search',
                          onPressed: () {
                            _searchController.clear();
                            _loadTopAnime();
                          },
                          icon: const Icon(Icons.close_rounded),
                        ),
                  filled: true,
                  fillColor: const Color(0xFF171720),
                  contentPadding: EdgeInsets.symmetric(vertical: isTvLayout ? 22 : 17),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(18),
                    borderSide: BorderSide.none,
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(18),
                    borderSide: const BorderSide(color: Color(0xFF292638)),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(18),
                    borderSide: const BorderSide(
                      color: Color(0xFF8B5CF6),
                      width: 1.5,
                    ),
                  ),
                ),
              ),
            ),
            Padding(
              padding: EdgeInsets.fromLTRB(pageHorizontalPadding, 0, pageHorizontalPadding, isTvLayout ? 20 : 12),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      isWatchlist
                          ? 'My List'
                          : _searchMode
                              ? 'Search results'
                              : 'Discover anime',
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                            fontWeight: FontWeight.w900,
                            letterSpacing: -0.4,
                          ),
                    ),
                  ),
                  if (!isWatchlist)
                    Text(
                      '${_anime.length} titles',
                      style: Theme.of(context)
                          .textTheme
                          .labelMedium
                          ?.copyWith(color: Colors.white60),
                    ),
                ],
              ),
            ),
            SizedBox(
              height: isTvLayout ? 54 : 42,
              child: ListView(
                padding: EdgeInsets.symmetric(horizontal: pageHorizontalPadding),
                scrollDirection: Axis.horizontal,
                children: [
                  _filterChip('All', Icons.grid_view_rounded),
                  const SizedBox(width: 8),
                  _filterChip('Top rated', Icons.local_fire_department_rounded),
                  const SizedBox(width: 8),
                  _filterChip('Short series', Icons.bolt_rounded),
                ],
              ),
            ),
            const SizedBox(height: 10),
            Expanded(
              child: AnimatedSwitcher(
                duration: const Duration(milliseconds: 240),
                child: _buildContent(
                  key: ValueKey('$_destination-$_filter-$_loading-$_error'),
                ),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _destination,
        onDestinationSelected: (index) {
          setState(() {
            _destination = index;
            _filter = 'All';
          });
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.explore_outlined),
            selectedIcon: Icon(Icons.explore_rounded),
            label: 'Discover',
          ),
          NavigationDestination(
            icon: Icon(Icons.bookmark_border_rounded),
            selectedIcon: Icon(Icons.bookmark_rounded),
            label: 'My List',
          ),
        ],
      ),
    );
  }

  Widget _filterChip(String label, IconData icon) {
    final selected = _filter == label;
    return ChoiceChip(
      selected: selected,
      showCheckmark: false,
      avatar: Icon(icon, size: 17),
      label: Text(label),
      onSelected: (_) => setState(() => _filter = label),
      selectedColor: const Color(0xFF6D28D9),
      backgroundColor: const Color(0xFF171720),
      side: BorderSide(
        color: selected ? const Color(0xFF8B5CF6) : const Color(0xFF302B3D),
      ),
      labelStyle: TextStyle(
        fontWeight: selected ? FontWeight.w800 : FontWeight.w500,
        color: Colors.white,
      ),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
    );
  }

  Widget _buildContent({required Key key}) {
    if (_loading && _destination == 0) {
      return const Center(
        key: ValueKey('loading'),
        child: CircularProgressIndicator(),
      );
    }
    if (_error != null && _destination == 0) {
      return Center(
        key: const ValueKey('error'),
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off_rounded, size: 46),
              const SizedBox(height: 12),
              Text(_error!, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: _searchMode ? _search : _loadTopAnime,
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('Try again'),
              ),
            ],
          ),
        ),
      );
    }

    final anime = _visibleAnime;
    if (anime.isEmpty) {
      return Center(
        key: const ValueKey('empty'),
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                _destination == 1
                    ? Icons.bookmark_add_outlined
                    : Icons.search_off_rounded,
                size: 54,
                color: Colors.white54,
              ),
              const SizedBox(height: 14),
              Text(
                _destination == 1
                    ? 'Your list is waiting'
                    : 'No matching anime found',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
              ),
              const SizedBox(height: 6),
              Text(
                _destination == 1
                    ? 'Tap the bookmark on any anime poster to save it here.'
                    : 'Try another filter or search using an English title.',
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white60),
              ),
              if (_destination == 1) ...[
                const SizedBox(height: 18),
                FilledButton.icon(
                  onPressed: () => setState(() => _destination = 0),
                  icon: const Icon(Icons.explore_rounded),
                  label: const Text('Explore anime'),
                ),
              ],
            ],
          ),
        ),
      );
    }

    final showHero = _destination == 0 && !_searchMode && _filter == 'All';
    return RefreshIndicator(
      key: key,
      onRefresh: _destination == 1 ? () async {} : _loadTopAnime,
      color: const Color(0xFFB794F6),
      child: CustomScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        slivers: [
          if (showHero && anime.isNotEmpty)
            SliverToBoxAdapter(child: _featuredCard(anime.first)),
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 28),
            sliver: SliverGrid(
              gridDelegate: SliverGridDelegateWithMaxCrossAxisExtent(
                maxCrossAxisExtent: MediaQuery.sizeOf(context).width >= 1400 ? 280 : MediaQuery.sizeOf(context).width >= 1000 ? 250 : 230,
                mainAxisSpacing: MediaQuery.sizeOf(context).width >= 1000 ? 24 : 18,
                crossAxisSpacing: MediaQuery.sizeOf(context).width >= 1000 ? 20 : 13,
                childAspectRatio: MediaQuery.sizeOf(context).width >= 1000 ? 0.68 : 0.57,
              ),
              delegate: SliverChildBuilderDelegate(
                (context, index) => _animeCard(anime[index]),
                childCount: anime.length,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _featuredCard(Anime anime) {
    return Padding(
      padding: EdgeInsets.fromLTRB(MediaQuery.sizeOf(context).width >= 1000 ? 36 : 16, 4, MediaQuery.sizeOf(context).width >= 1000 ? 36 : 16, 16),
      child: SizedBox(
        height: MediaQuery.sizeOf(context).width >= 1000 ? 300 : 210,
        child: Material(
          color: const Color(0xFF20182E),
          borderRadius: BorderRadius.circular(24),
          clipBehavior: Clip.antiAlias,
          child: InkWell(
            autofocus: false,
            focusColor: const Color(0x447C3AED),
            onTap: () => _openAnime(anime),
            child: Stack(
              fit: StackFit.expand,
              children: [
                if (anime.imageUrl != null)
                  Image.network(
                    anime.imageUrl!,
                    fit: BoxFit.cover,
                    alignment: Alignment.topCenter,
                    errorBuilder: (_, __, ___) => _posterPlaceholder(),
                  )
                else
                  _posterPlaceholder(),
                const DecoratedBox(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.centerRight,
                      end: Alignment.centerLeft,
                      colors: [
                        Colors.transparent,
                        Color(0xB809090F),
                        Color(0xF009090F),
                      ],
                      stops: [0.0, 0.48, 1.0],
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(18),
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: SizedBox(
                      width: MediaQuery.sizeOf(context).width >= 1000 ? 420 : 225,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 10, vertical: 5),
                            decoration: BoxDecoration(
                              color: const Color(0xFF7C3AED),
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: const Text(
                              'FEATURED ANIME',
                              style: TextStyle(
                                fontSize: 10,
                                letterSpacing: 1.1,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                          ),
                          const SizedBox(height: 10),
                          Text(
                            anime.englishTitle,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: MediaQuery.sizeOf(context).width >= 1000 ? 32 : 21,
                              height: 1.1,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                          const SizedBox(height: 9),
                          Row(
                            children: [
                              if (anime.score != null) ...[
                                const Icon(Icons.star_rounded,
                                    color: Color(0xFFFBBF24), size: 17),
                                const SizedBox(width: 4),
                                Text(anime.score!.toStringAsFixed(1)),
                                const SizedBox(width: 10),
                              ],
                              const Icon(Icons.play_circle_outline_rounded,
                                  size: 17),
                              const SizedBox(width: 5),
                              const Text('View details'),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                Positioned(
                  right: 10,
                  top: 10,
                  child: _watchlistButton(anime, compact: true),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _animeCard(Anime anime) {
    final saved = _watchlist.containsKey(anime.id);
    return Material(
      color: const Color(0xFF111118),
      borderRadius: BorderRadius.circular(17),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        autofocus: false,
        focusColor: const Color(0x447C3AED),
        onTap: () => _openAnime(anime),
        borderRadius: BorderRadius.circular(17),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Stack(
                fit: StackFit.expand,
                children: [
                  Hero(
                    tag: 'anime-poster-${anime.id}',
                    child: anime.imageUrl != null
                        ? Image.network(
                            anime.imageUrl!,
                            fit: BoxFit.cover,
                            errorBuilder: (_, __, ___) => _posterPlaceholder(),
                            loadingBuilder: (context, child, progress) {
                              if (progress == null) return child;
                              return Container(
                                color: const Color(0xFF20202B),
                                alignment: Alignment.center,
                                child: const SizedBox(
                                  width: 22,
                                  height: 22,
                                  child: CircularProgressIndicator(
                                      strokeWidth: 2),
                                ),
                              );
                            },
                          )
                        : _posterPlaceholder(),
                  ),
                  Positioned(
                    top: 8,
                    left: 8,
                    child: _ratingBadge(anime),
                  ),
                  Positioned(
                    top: 5,
                    right: 5,
                    child: _watchlistButton(anime),
                  ),
                  Positioned(
                    left: 0,
                    right: 0,
                    bottom: 0,
                    child: Container(
                      height: 64,
                      decoration: const BoxDecoration(
                        gradient: LinearGradient(
                          begin: Alignment.topCenter,
                          end: Alignment.bottomCenter,
                          colors: [Colors.transparent, Color(0xCC09090F)],
                        ),
                      ),
                    ),
                  ),
                  const Positioned(
                    left: 10,
                    bottom: 10,
                    child: Icon(Icons.play_circle_fill_rounded,
                        size: 30, color: Colors.white),
                  ),
                  if (saved)
                    const Positioned(
                      right: 9,
                      bottom: 11,
                      child: Icon(Icons.bookmark_rounded,
                          size: 18, color: Color(0xFFC4B5FD)),
                    ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(9, 9, 9, 2),
              child: Text(
                anime.englishTitle,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontWeight: FontWeight.w800,
                  fontSize: 13,
                  height: 1.2,
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(9, 4, 9, 10),
              child: Text(
                anime.episodeCount == null
                    ? 'Episode count unavailable'
                    : '${anime.episodeCount} episodes',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(color: Colors.white60, fontSize: 11),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _ratingBadge(Anime anime) {
    if (anime.score == null) return const SizedBox.shrink();
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 5),
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.78),
        borderRadius: BorderRadius.circular(9),
        border: Border.all(color: Colors.white.withValues(alpha: 0.12)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.star_rounded,
              color: Color(0xFFFBBF24), size: 14),
          const SizedBox(width: 3),
          Text(
            anime.score!.toStringAsFixed(1),
            style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w800),
          ),
        ],
      ),
    );
  }

  Widget _watchlistButton(Anime anime, {bool compact = false}) {
    final saved = _watchlist.containsKey(anime.id);
    return Material(
      color: Colors.black.withValues(alpha: 0.68),
      shape: const CircleBorder(),
      child: IconButton(
        visualDensity: VisualDensity.compact,
        tooltip: saved ? 'Remove from My List' : 'Add to My List',
        iconSize: compact ? 20 : 19,
        onPressed: () => _toggleWatchlist(anime),
        icon: Icon(
          saved ? Icons.bookmark_rounded : Icons.bookmark_add_outlined,
          color: saved ? const Color(0xFFC4B5FD) : Colors.white,
        ),
      ),
    );
  }

  Widget _posterPlaceholder() => Container(
        color: const Color(0xFF20202B),
        alignment: Alignment.center,
        child: const Icon(Icons.movie_outlined, size: 44),
      );
}
