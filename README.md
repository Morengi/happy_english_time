# EnglishPro — Платформа для изучения английского

Веб-платформа для изучения английского языка и проверки словарного запаса.

## Стек

- **Backend**: Java 17, Spring Boot 3.2, Spring Security (JWT), Spring WebSocket
- **Frontend**: Vue.js 3, Vite, Pinia, SCSS
- **БД**: PostgreSQL 16, Flyway (миграции)
- **Деплой**: Docker, nginx

## Роли

| Роль | Возможности |
|------|-------------|
| **Студент** | Словарный запас, тесты, занятия (по доступу), группы (как участник), личные сообщения |
| **Преподаватель** | Всё как у студента + создание групп/занятий, управление участниками, добавление слов для группы/занятия |
| **Администратор** | Полный доступ: управление пользователями, модерация занятий/групп |

## Локальный запуск

### Требования
- Java 17+
- Maven 3.9+
- Node.js 20+
- Docker (для PostgreSQL)

### Быстрый старт

```bash
# 1. Запустить PostgreSQL через Docker
docker compose up -d postgres

# 2. Backend
cd backend
mvn spring-boot:run

# 3. Frontend (в другом терминале)
cd frontend
npm install
npm run dev
```

Или одной командой:
```bash
./start-local.sh
```

Откройте: http://localhost:3000

**Администратор**: `admin` / `Admin@123`

## Структура проекта

```
english-platform/
├── backend/                  # Spring Boot приложение
│   └── src/main/java/com/englishplatform/
│       ├── config/           # SecurityConfig, WebSocketConfig
│       ├── controller/       # REST контроллеры
│       ├── dto/              # Request/Response DTO
│       ├── entity/           # JPA сущности
│       ├── repository/       # Spring Data репозитории
│       ├── security/         # JWT сервис и фильтр
│       └── service/          # Бизнес-логика
├── frontend/                 # Vue.js приложение
│   └── src/
│       ├── api/              # Axios клиент
│       ├── assets/styles/    # SCSS переменные и глобальные стили
│       ├── components/       # Переиспользуемые компоненты
│       ├── router/           # Vue Router
│       ├── stores/           # Pinia stores
│       └── views/            # Страницы приложения
├── docker-compose.yml
└── start-local.sh
```

## API

| Метод | URL | Описание |
|-------|-----|----------|
| POST | /api/auth/login | Авторизация |
| GET | /api/auth/me | Текущий пользователь |
| GET | /api/vocabulary | Словарный запас |
| POST | /api/vocabulary | Добавить слово |
| GET | /api/test/words | Получить слова для теста |
| POST | /api/test/submit | Отправить результаты |
| GET | /api/groups | Список групп |
| GET | /api/lessons | Список занятий |
| POST | /api/messages | Отправить сообщение |
| GET | /api/admin/users | Список пользователей (ADMIN) |
