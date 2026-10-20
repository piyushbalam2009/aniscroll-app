import 'package:flutter/material.dart';
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
  bool _loading = true;
  String? _error;
  int _episode = 1;
  String _track = 'sub';

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
            // Block top-level external redirects and non-HTTPS schemes.
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

  Future<void> _loadPlayer() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    await _controller.loadRequest(_playerUri);
  }

  Future<void> _changeEpisode(int delta) async {
    final next = _episode + delta;
    if (next < 1) return;
    setState(() => _episode = next);
    await _loadPlayer();
  }

  Future<void> _changeTrack(String track) async {
    if (track == _track) return;
    setState(() => _track = track);
    await _loadPlayer();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.anime.englishTitle,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ),
      body: Column(
        children: [
          AspectRatio(
            aspectRatio: 16 / 9,
            child: Stack(
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
                                onPressed: _loadPlayer,
                                child: const Text('Retry'),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.anime.englishTitle,
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 12),
                Wrap(
                  crossAxisAlignment: WrapCrossAlignment.center,
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    const Text('Episode'),
                    IconButton(
                      tooltip: 'Previous episode',
                      onPressed: _episode > 1 ? () => _changeEpisode(-1) : null,
                      icon: const Icon(Icons.remove_circle_outline),
                    ),
                    Text(
                      '$_episode',
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                    IconButton(
                      tooltip: 'Next episode',
                      onPressed: () => _changeEpisode(1),
                      icon: const Icon(Icons.add_circle_outline),
                    ),
                    const SizedBox(width: 12),
                    SegmentedButton<String>(
                      segments: const [
                        ButtonSegment(value: 'sub', label: Text('Sub')),
                        ButtonSegment(value: 'dub', label: Text('Dub')),
                      ],
                      selected: {_track},
                      onSelectionChanged: (selection) {
                        _changeTrack(selection.first);
                      },
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  'Streaming via ZokoAnime only. Pop-up and external navigation '
                  'blocking is best-effort; ads rendered inside the provider '
                  'player may still appear.',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Colors.white70,
                      ),
                ),
              ],
            ),
          ),
          if (widget.anime.synopsis.isNotEmpty)
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                child: Text(widget.anime.synopsis),
              ),
            )
          else
            const Spacer(),
        ],
      ),
    );
  }
}
