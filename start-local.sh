#!/bin/bash
set -e

echo "=== EnglishPro — Локальный запуск ==="
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ── Load user PATH (for tools installed via Homebrew / sdkman / etc.) ─────────
[ -f "$HOME/.zshrc" ]   && source "$HOME/.zshrc"   2>/dev/null || true
[ -f "$HOME/.bashrc" ]  && source "$HOME/.bashrc"  2>/dev/null || true
[ -f "$HOME/.profile" ] && source "$HOME/.profile" 2>/dev/null || true

# Add common Homebrew locations
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

# ── Locate mvn ────────────────────────────────────────────────────────────────
MVN=""
for candidate in \
    "$(which mvn 2>/dev/null)" \
    /opt/homebrew/bin/mvn \
    /usr/local/bin/mvn \
    /usr/local/opt/maven/bin/mvn \
    "$HOME/.sdkman/candidates/maven/current/bin/mvn"
do
  if [ -x "$candidate" ]; then
    MVN="$candidate"
    break
  fi
done

if [ -z "$MVN" ]; then
  echo ""
  echo "❌ Maven (mvn) не найден. Установите его:"
  echo ""
  echo "   macOS (Homebrew):  brew install maven"
  echo "   или скачайте с:    https://maven.apache.org/download.cgi"
  echo ""
  echo "   После установки снова запустите ./start-local.sh"
  exit 1
fi
echo "✅ Maven найден: $MVN"

# ── Check Docker ──────────────────────────────────────────────────────────────
if ! docker info > /dev/null 2>&1; then
  echo "❌ Docker не запущен. Запустите Docker Desktop."
  exit 1
fi

# ── PostgreSQL ────────────────────────────────────────────────────────────────
if lsof -i :5433 -sTCP:LISTEN -t > /dev/null 2>&1; then
  echo "⚠️  Порт 5433 занят. Пытаюсь остановить старый контейнер..."
  docker stop ep_postgres 2>/dev/null || true
  sleep 2
fi

echo "🐘 Запускаю PostgreSQL на порту 5433..."
cd "$ROOT_DIR"
docker compose up -d postgres

echo "⏳ Жду готовности PostgreSQL..."
for i in $(seq 1 30); do
  if docker exec ep_postgres pg_isready -U postgres > /dev/null 2>&1; then
    echo "✅ PostgreSQL готов!"
    break
  fi
  sleep 1
done

# ── Backend ───────────────────────────────────────────────────────────────────
echo ""
echo "☕ Запускаю Spring Boot backend (порт 8080)..."
cd "$ROOT_DIR/backend"
"$MVN" spring-boot:run > "$ROOT_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
echo "   PID backend: $BACKEND_PID"

echo "⏳ Жду готовности backend (до 90 сек)..."
for i in $(seq 1 45); do
  if curl -s -o /dev/null http://localhost:8080/api/auth/me 2>/dev/null; then
    echo "✅ Backend готов!"
    break
  fi
  sleep 2
  if ! kill -0 $BACKEND_PID 2>/dev/null; then
    echo "❌ Backend упал. Последние строки лога:"
    echo "────────────────────────────────────────"
    tail -40 "$ROOT_DIR/backend.log"
    echo "────────────────────────────────────────"
    echo "Полный лог: $ROOT_DIR/backend.log"
    exit 1
  fi
done

# ── Frontend ──────────────────────────────────────────────────────────────────
echo ""
echo "🌐 Запускаю Vue.js frontend (порт 3000)..."
cd "$ROOT_DIR/frontend"
if [ ! -d node_modules ]; then
  echo "📦 Устанавливаю зависимости npm..."
  npm install
fi
npm run dev > "$ROOT_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "   PID frontend: $FRONTEND_PID"
sleep 4

# ── Done ──────────────────────────────────────────────────────────────────────
echo ""
echo "=================================================="
echo "✅ Всё запущено!"
echo ""
echo "  🌐 Фронтенд:  http://localhost:3000"
echo "  ☕ Backend:   http://localhost:8080"
echo "  🗄️  PostgreSQL: localhost:5433"
echo ""
echo "  👤 Логин: admin  |  Пароль: Admin@123"
echo ""
echo "  📋 Логи:"
echo "     Backend:  tail -f $ROOT_DIR/backend.log"
echo "     Frontend: tail -f $ROOT_DIR/frontend.log"
echo "=================================================="
echo ""
echo "Нажмите Ctrl+C для остановки..."

cleanup() {
  echo ""
  echo "🛑 Останавливаю сервисы..."
  kill $FRONTEND_PID 2>/dev/null || true
  kill $BACKEND_PID  2>/dev/null || true
  cd "$ROOT_DIR" && docker compose stop postgres
  echo "Готово."
}
trap cleanup EXIT INT TERM

wait $BACKEND_PID $FRONTEND_PID
