#!/bin/bash

echo "=== EnglishPro — Локальный запуск ==="
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ── Add common tool paths without sourcing .zshrc (conda breaks set -e) ───────
export PATH="/opt/homebrew/bin:/opt/homebrew/sbin:/usr/local/bin:/usr/local/sbin:$PATH"

# ── Locate mvn (prefer local wrapper, then system mvn) ────────────────────────
MVNW="$ROOT_DIR/backend/mvnw"
[ -x "$MVNW" ] && MVN="$MVNW" || MVN=""

if [ -z "$MVN" ]; then
  for p in \
      "$(command -v mvn 2>/dev/null)" \
      /opt/homebrew/bin/mvn \
      /usr/local/bin/mvn \
      /usr/local/opt/maven/bin/mvn \
      "$HOME/.sdkman/candidates/maven/current/bin/mvn" \
      "$HOME/bin/mvn"
  do
    [ -x "$p" ] && MVN="$p" && break
  done
fi

if [ -z "$MVN" ]; then
  echo "❌ Maven не найден даже через wrapper. Нужна Java для работы ./mvnw"
  exit 1
fi
echo "✅ Maven wrapper: $MVN"

# ── Locate java ───────────────────────────────────────────────────────────────
if ! command -v java &>/dev/null && [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if ! command -v java &>/dev/null; then
  echo "❌ Java не найдена. Установите JDK 17+:"
  echo "   brew install openjdk@17"
  exit 1
fi
echo "✅ Java: $(java -version 2>&1 | head -1)"

# ── Locate node/npm ───────────────────────────────────────────────────────────
if ! command -v node &>/dev/null; then
  echo "❌ Node.js не найден. Установите:"
  echo "   brew install node"
  exit 1
fi
echo "✅ Node: $(node -v)"

# ── Check Docker ──────────────────────────────────────────────────────────────
if ! docker info > /dev/null 2>&1; then
  echo "❌ Docker не запущен. Запустите Docker Desktop и попробуйте снова."
  exit 1
fi
echo "✅ Docker запущен"

# ── PostgreSQL ────────────────────────────────────────────────────────────────
if lsof -i :5433 -sTCP:LISTEN -t > /dev/null 2>&1; then
  echo "⚠️  Порт 5433 занят. Останавливаю старый контейнер..."
  docker stop ep_postgres 2>/dev/null || true
  sleep 2
fi

echo ""
echo "🐘 Запускаю PostgreSQL (порт 5433)..."
cd "$ROOT_DIR"
docker compose up -d postgres 2>&1

echo "⏳ Жду готовности PostgreSQL..."
for i in $(seq 1 30); do
  if docker exec ep_postgres pg_isready -U postgres > /dev/null 2>&1; then
    echo "✅ PostgreSQL готов!"; break
  fi
  printf "."
  sleep 1
done
echo ""

# ── Backend ───────────────────────────────────────────────────────────────────
echo "☕ Запускаю Spring Boot backend (порт 8080)..."
cd "$ROOT_DIR/backend"
"$MVN" spring-boot:run > "$ROOT_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
echo "   PID: $BACKEND_PID  |  лог: backend.log"

echo "⏳ Жду готовности backend (до 90 сек)..."
BACKEND_OK=0
for i in $(seq 1 45); do
  if curl -s -o /dev/null http://localhost:8080/api/auth/me 2>/dev/null; then
    BACKEND_OK=1; break
  fi
  printf "."
  sleep 2
  if ! kill -0 $BACKEND_PID 2>/dev/null; then
    echo ""
    echo "❌ Backend упал. Последние строки:"
    echo "────────────────────────────────────────"
    tail -50 "$ROOT_DIR/backend.log"
    echo "────────────────────────────────────────"
    exit 1
  fi
done
echo ""
[ $BACKEND_OK -eq 1 ] && echo "✅ Backend готов!" || echo "⚠️  Backend долго стартует — проверьте backend.log"

# ── Frontend ──────────────────────────────────────────────────────────────────
echo ""
echo "🌐 Запускаю Vue.js frontend (порт 3000)..."
cd "$ROOT_DIR/frontend"
if [ ! -d node_modules ]; then
  echo "📦 Устанавливаю npm зависимости..."
  npm install
fi
npm run dev > "$ROOT_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "   PID: $FRONTEND_PID  |  лог: frontend.log"
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
echo "  Логи:"
echo "    tail -f $ROOT_DIR/backend.log"
echo "    tail -f $ROOT_DIR/frontend.log"
echo "=================================================="
echo ""
echo "Нажмите Ctrl+C для остановки всех сервисов..."

cleanup() {
  echo ""
  echo "🛑 Останавливаю..."
  kill $FRONTEND_PID 2>/dev/null || true
  kill $BACKEND_PID  2>/dev/null || true
  cd "$ROOT_DIR" && docker compose stop postgres 2>/dev/null || true
  echo "Готово."
}
trap cleanup EXIT INT TERM

wait $BACKEND_PID $FRONTEND_PID
