# FrameGuard: сборка с телефона

1. Зайди на github.com (браузер, версия для ПК), создай репозиторий (New repository, любое имя).
2. Add file → Upload files → загрузи ВСЕ файлы, КРОМЕ build.yml:
   build.gradle, settings.gradle, neoforge.mods.toml, Config.java, IrisBridge.java,
   QualityController.java, FrameGuard.java
3. Вкладка Actions → "set up a workflow yourself" → удали шаблон, вставь содержимое build.yml → Commit.
4. Actions → build → Run workflow (если сам не запустился).
5. Через 3-5 минут открой завершённый запуск → внизу Artifacts → frameguard-jar (скачается zip, внутри .jar).
6. Положи .jar в папку mods профиля, запусти игру.

Команды в игре: /frameguard (статус), /frameguard on, /frameguard off
Конфиг: config/frameguard-client.toml (целевой FPS, пороги, разрешения на шейдеры и дальность).

Если сборка упала, открой лог шага "Сборка" и пришли мне текст ошибки.
