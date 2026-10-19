

 REST API на Java 21, Spring Boot 4.1.1, Spring Security, JWT и PostgreSQL. В проект включены интеграционные тесты, SAST-анализатор SpotBugs и SCA-проверка OWASP Dependency-Check для GitHub Actions.


## API

Все запросы и ответы используют JSON. За исключением `POST /auth/**`, методы `/api/**` требуют заголовок `Authorization: Bearer <JWT>`.

| Метод | Endpoint | Назначение | Доступ |
|---|---|---|---|
| `POST` | `/auth/login` | Проверяет имя пользователя и пароль, возвращает JWT | Публичный |
| `GET` | `/api/data` | Возвращает заметки текущего пользователя | JWT |
| `POST` | `/api/data` | Создаёт заметку от имени текущего пользователя | JWT |

### `POST /auth/login`

Запрос:

```json
{
  "username": "demo",
  "password": "LocalOnly_ChangeMe_123!"
}
```

Пример успешного ответа (`200 OK`):

```json
{
  "tokenType": "Bearer",
  "accessToken": "<JWT>",
  "expiresIn": 900
}
```

Неверная пара логин/пароль возвращает `401 Unauthorized`; сообщение не раскрывает, какое именно поле оказалось неверным. Тело запроса валидируется, имя пользователя ограничено допустимым набором символов, длина пароля ограничена.

### `GET /api/data`

```bash
curl -i http://localhost:8080/api/data

curl -s http://localhost:8080/api/data \
  -H "Authorization: Bearer $TOKEN"
```

Запрос без токена должен вернуть `401 Unauthorized`. Ответ включает только данные, у которых владелец совпадает с именем пользователя из проверенного JWT.

### `POST /api/data`

```bash
curl -i http://localhost:8080/api/data \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Покупки","content":"Молоко и хлеб"}'
```

Успешное создание заметки возвращает `201 Created`. Поля `title` и `content` обязательны. HTML-разметка удаляется, а текстовые поля дополнительно экранируются перед сериализацией ответа.

## Реализованные меры защиты

### SQL Injection (SQLi)

Доступ к базе реализован через Spring Data JPA/Hibernate и репозитории. Запросы типа `findByUsername` и `findAllByOwnerUsernameOrderByCreatedAtDesc` передают пользовательские значения как параметры. SQL-строки не формируются конкатенацией пользовательского ввода. При добавлении произвольных native SQL-запросов необходимо использовать параметры (`:name`, `?`) и не вставлять ввод непосредственно в SQL.

### Cross-Site Scripting (XSS)

Перед сохранением заметки библиотека Jsoup удаляет HTML-разметку по политике `Safelist.none()`. Поля, возвращаемые API, экранируются `HtmlUtils.htmlEscape`. Это учебное применение output encoding; клиенты всё равно должны отображать полученные значения как текст (`textContent`), а не вставлять их как доверенный HTML. В API не возвращаются HTML-страницы, а ответы контроллеров сериализуются как JSON.

### Broken Authentication

- Проверка пароля выполняется Spring Security `DaoAuthenticationProvider` и `BCryptPasswordEncoder` с cost factor 12; пароль не хранится в открытом виде.
- После успешного входа выдаётся подписанный JWT с subject, временем выпуска и временем истечения. Срок действия по умолчанию — 15 минут.
- `JwtAuthenticationFilter` проверяет подпись, срок действия и существование пользователя и устанавливает Spring Security context. Неподписанный, неверный или просроченный токен не даёт доступа.
- Сессии на сервере не используются (`STATELESS`); доступ ко всем `/api/**` требует аутентификации.
- Ошибки входа не раскрывают, существует ли указанный пользователь. Для публичного развёртывания дополнительно добавьте rate limiting/защиту от credential stuffing.

### Дополнительные настройки

- DTO и Bean Validation ограничивают размер и допустимый формат входных данных.
- Ошибки приложения не содержат stack trace и подробности привязки полей в HTTP-ответах.
- Добавлены защитные HTTP-заголовки `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` и `Cache-Control`.
- `.env` исключён из Git. В репозитории хранится только `.env.example` с демонстрационными значениями.
- В Docker-образе приложение работает под непривилегированным пользователем.

## CI/CD: SAST и SCA

Файл `.github/workflows/ci.yml` запускает проверки при `push` в ветки `main`/`master`, при каждом `pull_request`, а также вручную через `workflow_dispatch`.

- **SAST — SpotBugs:** запускается в фазе `verify`; ошибки анализатора с высоким приоритетом приводят к неуспешному job.
- **SCA — OWASP Dependency-Check:** сканирует прямые и транзитивные Maven-зависимости по публичным уязвимостям CVE. По умолчанию порог завершения сборки — CVSS 7.0.
- Оба job публикуют отчёты в разделе Artifacts конкретного GitHub Actions run, в том числе если проверка завершилась ошибкой.


