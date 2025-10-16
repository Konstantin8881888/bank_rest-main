### 📚 API Документация

Доступные эндпоинты:

Аутентификация:

POST /api/auth/register - Регистрация нового пользователя

POST /api/auth/login - Вход в систему

Управление картами (требуют аутентификации):

GET /api/cards/my - Получить свои карты (USER/ADMIN)

GET /api/cards/my/{id} - Получить конкретную свою карту (USER/ADMIN)

PUT /api/cards/my/{id}/block - Заблокировать свою карту (USER/ADMIN)

POST /api/cards/transfer - Перевод между своими картами (USER/ADMIN)

Административные эндпоинты (только ADMIN):

GET /api/cards - Получить все карты

POST /api/cards - Создать новую карту

PUT /api/cards/{id}/block - Блокировка карты

PUT /api/cards/{id}/activate - Активация карты

DELETE /api/cards/{id} - Удаление карты

Полная документация API доступна через Swagger UI:

http://localhost:8080/swagger-ui.html
