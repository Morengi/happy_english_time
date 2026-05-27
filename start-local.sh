#!/bin/bash
set -e

echo "=== EnglishPro — Локальный запуск ==="
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Check Docker
if ! docker info > /dev/null 2>&1; then
  echo "❌ Docker не запущен. Запустите Docker Desktop."
  exit 1
fi

# Check if port 5433 is free; if not, stop any leftover container
if lsof -i :5433 -sTCP:LISTEN -t > /dev/null 2>&1; then
  echo "⚠️  Порт 5433 занят. Пытаюсь остановить старый контейнер..."
  docker stop ep_postgres 2>/dev/null || true
  sleep 2
fi

# Start PostgreSQL on port 5433 (avoids conflict with local PG on 5432)
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

# Start backend
echo ""
echo "☕ Запускаю Spring Boot backend (порт 8080)..."
cd "$ROOT_DIR/backend"
mvn spring-boot:run > "$ROOT_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
echo "   PID backend: $BACKEND_PID"

# Wait for backend
echo "⏳ Жду готовности backend..."
for i in $(seq 1 60); do
  if curl -s http://localhost:8080/api/auth/me > /dev/null 2>&1; then
    echo "✅ Backend готов!"
    break
  fi
  sleep 2
  if ! kill -0 $BACKEND_PID 2>/dev/null; then
    echo "❌ Backend упал. Смотри лог: $ROOT_DIR/backend.log"
    cat "$ROOT_DIR/backend.log" | tail -30
    exit 1
  fi
done

# Start frontend
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

echo ""
echo "=================================================="
echo "✅ Всё запущено!"
echo ""
echo "  🌐 Фронтенд:  http://localhost:3000"
echo "  ☕ Backend:   http://localhost:8080"
echo "  🗄️  PostgreSQL: localhost:5433 (db: english_platform)"
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
  kill $BACKEND_PID 2>/dev/null || true
  cd "$ROOT_DIR" && docker compose stop postgres
  echo "Готово."
}
trap cleanup EXIT INT TERM

wait $BACKEND_PID $FRONTEND_PID
