# selekt-sqlite3-ext
Scaffolding module for SQLite extension support in Selekt.
## Scope
This module currently provides:
- `SQLiteExtension` descriptor type
- `SQLiteExtensionSql.loadExtensionStatement(...)` SQL builder with escaping
This keeps extension SQL generation centralized and reusable from Java and Android layers.
## Next steps
- Add native-level extension loading APIs if required.
- Add policy controls around allowed extension paths / signatures.
- Add integration tests that load a real extension in CI environments that support it.
