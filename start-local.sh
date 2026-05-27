#!/bin/bash
set -e

echo "=== EnglishPro — Локальный запуск ==="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
  echo "❌ Docker не запущен. Запустите Docker Desktop."
  exit 1
fi

# Start PostgreSQL only via Docker
echo "🐘 Запускаю PostgreSQL..."
docker compose up -d postgres

echo "⏳ Жду готовности PostgreSQL..."
sleep 5

# Start backend
echo "☕ Запускаю Spring Boot backend..."
cd backend
mvn spring-boot:run &
BACKEND_PID=$!
cd ..

# Wait for backend
echo "⏳ Жду готовности backend (30 сек)..."
sleep 30

# Start frontend
echo "🌐 Запускаю Vue.js frontend..."
cd frontend
npm install
npm run dev &
FRONTEND_PID=$!
cd ..

echo ""
echo "✅ Всё запущено!"
echo "  🌐 Фронтенд:  http://localhost:3000"
echo "  ☕ Backend:   http://localhost:8080"
echo "  🗄️  PostgreSQL: localhost:5432"
echo ""
echo "  👤 Логин администратора: admin / Admin@123"
echo ""
echo "Нажмите Ctrl+C для остановки..."

wait $BACKEND_PID $FRONTEND_PID
