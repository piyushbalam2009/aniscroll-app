import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:webview_flutter/webview_flutter.dart';

import '../models/anime.dart';

class PlayerScreen extends StatefulWidget {
  const PlayerScreen({super.key, required this.anime});

  final Anime anime;

  @override
  State<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends State<PlayerScreen> {
  late final WebViewController _controller;
  final TextEditingController _episodeSearchController = TextEditingController();

  bool _loading = true;
  bool _fullscreen = false;
  bool _fullscreenWasActive = false;
  String? _error;
  int _episode = 1;
  int _rangeStart = 1;
  String _track = 'sub';
  Set<String> _availableTracks = {'sub'};
  bool _checkingTracks = true;
  int _trackCheckToken = 0;

  int get _episodeCount => widget.anime.episodeCount ?? 0;
  bool get _useEpisodeGrid => _episodeCount > 50;
  int get _rangeEnd => (_rangeStart + 99).clamp(1, _episodeCount).toInt();

  Uri get _playerUri => Uri.https(
        'zokoanime.video',
        '/stream/mal/${widget.anime.id}/$_episode/$_track',
        const {'color': '8b5cf6'},
      );

  bool _isAllowedProvider(Uri uri) {
    final host = uri.host.toLowerCase();
    return uri.scheme == 'https' &&
        (host == 'zokoanime.video' || host.endsWith('.zokoanime.video'));
  }

  @override
  void initState() {
    super.initState();
    _configureWebView();
    _detectAvailableTracks();
  }

  @override
  void dispose() {
    if (_fullscreenWasActive) {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
      SystemChrome.setPreferredOrientations(DeviceOrientation.values);
    }
    _episodeSearchController.dispose();
    super.dispose();
  }

  void _configureWebView() {
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(const Color(0xFF09090F))
      ..setNavigationDelegate(
        NavigationDelegate(
          onNavigationRequest: (request) {
            final uri = Uri.tryParse(request.url);
            if (uri != null && _isAllowedProvider(uri)) {
              return NavigationDecision.navigate;
            }
            return NavigationDecision.prevent;
          },
          onPageStarted: (_) {
            if (mounted) {
              setState(() {
                _loading = true;
                _error = null;
              });
            }
          },
          onPageFinished: (_) {
            if (mounted) setState(() => _loading = false);
          },
          onWebResourceError: (error) {
            if (error.isForMainFrame == true && mounted) {
              setState(() {
                _loading = false;
                _error = 'The ZokoAnime page could not be loaded. Check your '
                    'connection and try again.';
              });
            }
          },
        ),
      )
      ..loadRequest(_playerUri);
  }


  Future<bool> _probeTrack(String track) async {
    final uri = Uri.https(
      'zokoanime.video',
      '/stream/mal/${widget.anime.id}/$_episode/$track',
      const {'color': '8b5cf6'},
    );
    try {
      final response = await http.get(
        uri,
        headers: const {'Accept': 'text/html'},
      ).timeout(const Duration(seconds: 5));
      if (response.statusCode < 200 || response.statusCode >= 300) {
        return false;
      }
      final body = utf8.decode(response.bodyBytes, allowMalformed: true)
          .toLowerCase();
      const unavailableMarkers = [
        'episode not found',
        'no episode found',
        'stream not available',
        'not available in this language',
        'dub not available',
        'sub not available',
        'no sources found',
        'content unavailable',
      ];
      if (unavailableMarkers.any(body.contains)) return false;
      return body.contains('video') ||
          body.contains('player') ||
          body.contains('hls') ||
          body.contains('window.__p');
    } catch (_) {
      return false;
    }
  }

  Future<void> _detectAvailableTracks() async {
    final token = ++_trackCheckToken;
    if (mounted) setState(() => _checkingTracks = true);
    final results = await Future.wait([
      _probeTrack('sub'),
      _probeTrack('dub'),
    ]);
    if (!mounted || token != _trackCheckToken) return;
    final available = <String>{
      if (results[0]) 'sub',
      if (results[1]) 'dub',
    };
    if (available.isEmpty) available.add(_track);
    final selected = available.contains(_track) ? _track : available.first;
    final trackChanged = selected != _track;
    setState(() {
      _availableTracks = available;
      _track = selected;
      _checkingTracks = false;
    });
    if (trackChanged) await _loadPlayer();
  }

  Future<void> _loadPlayer() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    await _controller.loadRequest(_playerUri);
  }

  Future<void> _selectEpisode(int episode) async {
    if (episode < 1 || (_episodeCount > 0 && episode > _episodeCount)) return;
    if (episode == _episode) return;
    setState(() {
      _episode = episode;
      if (_useEpisodeGrid) {
        _rangeStart = ((episode - 1) ~/ 100) * 100 + 1;
      }
    });
    await _loadPlayer();
    await _detectAvailableTracks();
  }


  Future<void> _changeTrack(String track) async {
    if (track == _track) return;
    setState(() => _track = track);
    await _loadPlayer();
  }

  void _searchEpisode() {
    final episode = int.tryParse(_episodeSearchController.text.trim());
    if (episode == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter a valid episode number.')),
      );
      return;
    }
    if (_episodeCount > 0 && episode > _episodeCount) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('This anime has only $_episodeCount episodes.')),
      );
      return;
    }
    if (episode < 1) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Episode number must be at least 1.')),
      );
      return;
    }
    _selectEpisode(episode);
    FocusScope.of(context).unfocus();
  }

  Widget _buildEpisodeHeader() {
    return Row(
      children: [
        const Expanded(
          child: Text(
            'List of episodes',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
          ),
        ),
        SizedBox(
          width: 150,
          child: TextField(
            controller: _episodeSearchController,
            keyboardType: TextInputType.number,
            textInputAction: TextInputAction.search,
            onSubmitted: (_) => _searchEpisode(),
            decoration: InputDecoration(
              hintText: 'Number of Ep',
              prefixIcon: const Icon(Icons.search, size: 20),
              suffixIcon: IconButton(
                tooltip: 'Go to episode',
                onPressed: _searchEpisode,
                icon: const Icon(Icons.arrow_forward, size: 18),
              ),
              isDense: true,
              contentPadding: const EdgeInsets.symmetric(vertical: 10),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildEpisodeRow(int episode) {
    final selected = episode == _episode;
    final colors = Theme.of(context).colorScheme;
    return Material(
      color: selected ? colors.primary.withValues(alpha: 0.18) : Colors.white.withValues(alpha: episode.isEven ? 0.055 : 0.025),
      child: InkWell(
        onTap: () => _selectEpisode(episode),
        child: Container(
          constraints: const BoxConstraints(minHeight: 64),
          decoration: BoxDecoration(
            border: Border(
              left: BorderSide(
                color: selected ? colors.primary : Colors.transparent,
                width: 4,
              ),
            ),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Row(
            children: [
              SizedBox(
                width: 28,
                child: Text(
                  '$episode',
                  style: TextStyle(
                    color: selected ? colors.primary : Colors.white70,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  'Episode $episode',
                  style: TextStyle(
                    color: selected ? colors.primary : Colors.white,
                    fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                  ),
                ),
              ),
              if (selected)
                CircleAvatar(
                  radius: 17,
                  backgroundColor: colors.primary,
                  foregroundColor: colors.onPrimary,
                  child: const Icon(Icons.play_arrow_rounded, size: 22),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildEpisodeGrid() {
    final colors = Theme.of(context).colorScheme;
    final rangeCount = ((_episodeCount - 1) ~/ 100) + 1;
    final ranges = List<int>.generate(rangeCount, (index) => index * 100 + 1);
    final end = _rangeEnd;

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFF101019),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.tune_rounded, color: colors.primary),
              const SizedBox(width: 10),
              const Text('EPS:', style: TextStyle(fontWeight: FontWeight.w700)),
              const SizedBox(width: 10),
              Expanded(
                child: DropdownButtonFormField<int>(
                  initialValue: _rangeStart,
                  isExpanded: true,
                  decoration: const InputDecoration(
                    isDense: true,
                    contentPadding: EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                    border: OutlineInputBorder(),
                  ),
                  items: ranges.map((start) {
                    final finish = (start + 99).clamp(1, _episodeCount).toInt();
                    return DropdownMenuItem<int>(
                      value: start,
                      child: Text(
                        '${start.toString().padLeft(3, '0')}-${finish.toString().padLeft(3, '0')}',
                      ),
                    );
                  }).toList(),
                  onChanged: (start) {
                    if (start != null) setState(() => _rangeStart = start);
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: end - _rangeStart + 1,
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 5,
              mainAxisSpacing: 6,
              crossAxisSpacing: 6,
              childAspectRatio: 1.25,
            ),
            itemBuilder: (context, index) {
              final episode = _rangeStart + index;
              final selected = episode == _episode;
              return Material(
                color: selected ? colors.primary : const Color(0xFF30303B),
                borderRadius: BorderRadius.circular(5),
                child: InkWell(
                  borderRadius: BorderRadius.circular(5),
                  onTap: () => _selectEpisode(episode),
                  child: Center(
                    child: Text(
                      '$episode',
                      style: TextStyle(
                        color: selected ? colors.onPrimary : Colors.white70,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _buildEpisodeList() {
    if (_episodeCount <= 0) {
      return const Padding(
        padding: EdgeInsets.all(16),
        child: Text('Episode list is unavailable from MyAnimeList for this title.'),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildEpisodeHeader(),
        const SizedBox(height: 14),
        if (_useEpisodeGrid)
          _buildEpisodeGrid()
        else
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: Column(
              children: List<Widget>.generate(
                _episodeCount,
                (index) => _buildEpisodeRow(index + 1),
              ),
            ),
          ),
      ],
    );
  }

  bool get _isTvLayout => MediaQuery.sizeOf(context).width >= 800;

  Future<void> _toggleFullscreen() async {
    setState(() {
      _fullscreen = !_fullscreen;
      _fullscreenWasActive = _fullscreen;
    });
    await SystemChrome.setEnabledSystemUIMode(
      _fullscreen ? SystemUiMode.immersiveSticky : SystemUiMode.edgeToEdge,
    );
    await SystemChrome.setPreferredOrientations(
      _fullscreen
          ? const [DeviceOrientation.landscapeLeft, DeviceOrientation.landscapeRight]
          : DeviceOrientation.values,
    );
  }

  @override
  Widget build(BuildContext context) {
    final isTv = _isTvLayout;
    final player = SizedBox(
      height: _fullscreen && isTv ? double.infinity : null,
      child: _fullscreen && isTv
          ? Stack(
              fit: StackFit.expand,
              children: [
                WebViewWidget(controller: _controller),
                if (_loading)
                  const Align(alignment: Alignment.topCenter, child: LinearProgressIndicator(minHeight: 3)),
                if (_error != null)
                  Positioned.fill(
                    child: ColoredBox(
                      color: const Color(0xFF09090F),
                      child: Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [Text(_error!, textAlign: TextAlign.center), const SizedBox(height: 12), FilledButton(onPressed: _loadPlayer, child: const Text('Retry'))],
                        ),
                      ),
                    ),
                  ),
                Positioned(
                  right: 12,
                  top: 12,
                  child: Material(
                    color: Colors.black.withValues(alpha: 0.75),
                    borderRadius: BorderRadius.circular(30),
                    child: IconButton(
                      autofocus: true,
                      tooltip: 'Exit full screen',
                      onPressed: _toggleFullscreen,
                      icon: const Icon(Icons.fullscreen_exit),
                    ),
                  ),
                ),
              ],
            )
          : AspectRatio(
              aspectRatio: 16 / 9,
              child: Stack(
        fit: StackFit.expand,
        children: [
          WebViewWidget(controller: _controller),
          if (_loading)
            const Align(
              alignment: Alignment.topCenter,
              child: LinearProgressIndicator(minHeight: 3),
            ),
          if (_error != null)
            Positioned.fill(
              child: ColoredBox(
                color: const Color(0xFF09090F),
                child: Center(
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.warning_amber_rounded, size: 40),
                        const SizedBox(height: 10),
                        Text(_error!, textAlign: TextAlign.center),
                        const SizedBox(height: 12),
                        FilledButton(
                          autofocus: true,
                          onPressed: _loadPlayer,
                          child: const Text('Retry'),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          Positioned(
            right: 12,
            top: 12,
            child: Material(
              color: Colors.black.withValues(alpha: 0.75),
              borderRadius: BorderRadius.circular(30),
              child: IconButton(
                autofocus: false,
                tooltip: _fullscreen ? 'Exit full screen' : 'Full screen',
                onPressed: _toggleFullscreen,
                icon: Icon(_fullscreen ? Icons.fullscreen_exit : Icons.fullscreen),
              ),
            ),
          ),
        ],
              ),
            ),
    );

    if (_fullscreen && !isTv) {
      return Scaffold(
        backgroundColor: Colors.black,
        body: SafeArea(top: false, bottom: false, child: player),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.anime.englishTitle,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ),
      body: isTv
          ? (_fullscreen
              ? ColoredBox(color: Colors.black, child: SizedBox.expand(child: player))
              : Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  flex: 7,
                  child: ListView(
                    padding: const EdgeInsets.all(20),
                    children: [
                      player,
                      const SizedBox(height: 16),
                      Text(
                        widget.anime.englishTitle,
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      const SizedBox(height: 10),
                      Wrap(
                        spacing: 8,
                        children: [
                          if (_checkingTracks)
                            const Chip(label: Text('Checking audio...'))
                          else ...[
                            for (final track in ['sub', 'dub'])
                              if (_availableTracks.contains(track))
                                ChoiceChip(
                                  autofocus: track == _track,
                                  label: Text(track.toUpperCase()),
                                  selected: _track == track,
                                  onSelected: (_) => _changeTrack(track),
                                ),
                          ],
                        ],
                      ),
                      const SizedBox(height: 10),
                      Text(
                        'Use the remote to focus the player controls or choose an episode.',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                ),
                const VerticalDivider(width: 1),
                SizedBox(
                  width: 340,
                  child: ListView(
                    padding: const EdgeInsets.fromLTRB(12, 18, 12, 24),
                    children: [
                      Text('Episodes', style: Theme.of(context).textTheme.titleLarge),
                      const SizedBox(height: 12),
                      _buildEpisodeHeader(),
                      const SizedBox(height: 12),
                      if (_episodeCount > 50)
                        _buildEpisodeGrid()
                      else
                        ClipRRect(
                          borderRadius: BorderRadius.circular(8),
                          child: Column(
                            children: List<Widget>.generate(
                              _episodeCount,
                              (index) => _buildEpisodeRow(index + 1),
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
              ],
            ))
          : ListView(
              padding: const EdgeInsets.only(bottom: 24),
              children: [
                player,
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(widget.anime.englishTitle,
                          style: Theme.of(context).textTheme.titleLarge),
                      const SizedBox(height: 12),
                      Wrap(
                        crossAxisAlignment: WrapCrossAlignment.center,
                        spacing: 8,
                        runSpacing: 8,
                        children: [
                          if (_checkingTracks)
                            const SizedBox(
                              width: 22,
                              height: 22,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          else if (_availableTracks.length > 1)
                            SegmentedButton<String>(
                              segments: const [
                                ButtonSegment(value: 'sub', label: Text('Sub')),
                                ButtonSegment(value: 'dub', label: Text('Dub')),
                              ],
                              selected: {_track},
                              onSelectionChanged: (selection) =>
                                  _changeTrack(selection.first),
                            )
                          else
                            Chip(
                              avatar: const Icon(Icons.check_circle, size: 18),
                              label: Text(_availableTracks.contains('dub') ? 'Dub' : 'Sub'),
                              visualDensity: VisualDensity.compact,
                            ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Streaming via ZokoAnime only. Pop-up and external navigation '
                        'blocking is best-effort; ads rendered inside the provider '
                        'player may still appear.',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.white70),
                      ),
                      const SizedBox(height: 22),
                      _buildEpisodeList(),
                      if (widget.anime.synopsis.isNotEmpty) ...[
                        const SizedBox(height: 28),
                        Text('Synopsis',
                            style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
                        const SizedBox(height: 8),
                        Text(widget.anime.synopsis),
                      ],
                    ],
                  ),
                ),
              ],
            ),
    );
  }
}
