import 'package:flutter/material.dart';

class AdvancedSearchScreen extends StatefulWidget {
  const AdvancedSearchScreen({super.key});

  @override
  State<AdvancedSearchScreen> createState() => _AdvancedSearchScreenState();
}

class _AdvancedSearchScreenState extends State<AdvancedSearchScreen> {
  final _queryController = TextEditingController();
  String? _genre;
  String? _season;
  int? _year;
  String? _format;
  String _sort = 'Popularity';

  static const _genres = [
    'Action', 'Adventure', 'Avant Garde', 'Award Winning', 'Boys Love',
    'Comedy', 'Drama', 'Fantasy', 'Girls Love', 'Gourmet', 'Horror',
    'Mystery', 'Romance', 'Sci-Fi', 'Slice of Life', 'Sports', 'Supernatural',
    'Suspense', 'Ecchi'
  ];
  static const _seasons = ['winter', 'spring', 'summer', 'fall'];
  static const _formats = ['tv', 'movie', 'ova', 'ona', 'special', 'music'];
  static const _sortOptions = [
    'Popularity', 'Highest rated', 'Lowest rated', 'Newest first',
    'Oldest first', 'Title A–Z'
  ];

  @override
  void dispose() {
    _queryController.dispose();
    super.dispose();
  }

  void _apply() {
    Navigator.of(context).pop(<String, dynamic>{
      'query': _queryController.text.trim(),
      'genre': _genre,
      'season': _season,
      'year': _year,
      'format': _format,
      'sort': _sort,
    });
  }

  @override
  Widget build(BuildContext context) {
    final wide = MediaQuery.sizeOf(context).width >= 700;
    final years = List<int>.generate(40, (index) => DateTime.now().year - index);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Advanced search'),
        actions: [
          TextButton(
            onPressed: () => setState(() {
              _queryController.clear();
              _genre = null;
              _season = null;
              _year = null;
              _format = null;
              _sort = 'Popularity';
            }),
            child: const Text('Reset'),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 900),
          child: ListView(
            padding: EdgeInsets.all(wide ? 32 : 20),
            children: [
              const Text(
                'Find your next favorite',
                style: TextStyle(fontSize: 26, fontWeight: FontWeight.w900),
              ),
              const SizedBox(height: 8),
              const Text(
                'Combine filters to narrow down anime. Results use MyAnimeList metadata and only show titles with an English title.',
                style: TextStyle(color: Colors.white70, height: 1.4),
              ),
              const SizedBox(height: 24),
              TextField(
                controller: _queryController,
                textInputAction: TextInputAction.search,
                onSubmitted: (_) => _apply(),
                decoration: const InputDecoration(
                  labelText: 'Title or keyword',
                  hintText: 'e.g. fantasy adventure',
                  prefixIcon: Icon(Icons.search_rounded),
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 18),
              LayoutBuilder(
                builder: (context, constraints) {
                  final columns = constraints.maxWidth >= 620 ? 2 : 1;
                  final itemWidth = (constraints.maxWidth - (columns - 1) * 14) / columns;
                  return Wrap(
                    spacing: 14,
                    runSpacing: 16,
                    children: [
                      SizedBox(
                        width: itemWidth,
                        child: _dropdown<String>(
                          label: 'Genre',
                          value: _genre,
                          items: _genres,
                          itemLabel: (value) => value,
                          onChanged: (value) => setState(() => _genre = value),
                        ),
                      ),
                      SizedBox(
                        width: itemWidth,
                        child: _dropdown<String>(
                          label: 'Season',
                          value: _season,
                          items: _seasons,
                          itemLabel: (value) => value[0].toUpperCase() + value.substring(1),
                          onChanged: (value) => setState(() => _season = value),
                        ),
                      ),
                      SizedBox(
                        width: itemWidth,
                        child: _dropdown<int>(
                          label: 'Year',
                          value: _year,
                          items: years,
                          itemLabel: (value) => '$value',
                          onChanged: (value) => setState(() => _year = value),
                        ),
                      ),
                      SizedBox(
                        width: itemWidth,
                        child: _dropdown<String>(
                          label: 'Format',
                          value: _format,
                          items: _formats,
                          itemLabel: (value) => value.toUpperCase(),
                          onChanged: (value) => setState(() => _format = value),
                        ),
                      ),
                      SizedBox(
                        width: itemWidth,
                        child: _dropdown<String>(
                          label: 'Sort by',
                          value: _sort,
                          items: _sortOptions,
                          itemLabel: (value) => value,
                          allowAll: false,
                          onChanged: (value) {
                            if (value != null) setState(() => _sort = value);
                          },
                        ),
                      ),
                    ],
                  );
                },
              ),
              const SizedBox(height: 28),
              SizedBox(
                height: 54,
                child: FilledButton.icon(
                  onPressed: _apply,
                  icon: const Icon(Icons.tune_rounded),
                  label: const Text('Apply filters'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _dropdown<T>({
    required String label,
    required T? value,
    required List<T> items,
    required String Function(T) itemLabel,
    required ValueChanged<T?> onChanged,
    bool allowAll = true,
  }) {
    final values = allowAll ? <T?>[null, ...items] : items.cast<T?>();
    return DropdownButtonFormField<T>(
      initialValue: value,
      isExpanded: true,
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
      ),
      items: values.map((item) {
        return DropdownMenuItem<T>(
          value: item,
          child: Text(item == null ? 'Any $label' : itemLabel(item), overflow: TextOverflow.ellipsis),
        );
      }).toList(),
      onChanged: onChanged,
    );
  }
}
