# Gradient Pillars - PlaceholderAPI Placeholders

## Встановлення

1. Встановіть [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) на ваш сервер
2. Перезавантажте сервер
3. Плагін автоматично зареєструє свої плейсхолдери

## Доступні плейсхолдери

| Плейсхолдер    | Опис                     | Приклад виводу |
| -------------- | ------------------------ | -------------- |
| `%gp_wins%`    | Кількість перемог гравця | `15`           |
| `%gp_losses%`  | Кількість поразок гравця | `8`            |
| `%gp_total%`   | Загальна кількість ігор  | `23`           |
| `%gp_winrate%` | Відсоток перемог         | `65.2`         |

## Приклади використання

### В чаті (з DeluxeChat або іншим плагіном):

```
Формат: [{gp_wins}⭐] {player}: {message}
Результат: [15⭐] xlanyleeet: Привіт!
```

### На табличці:

```
[Stats]
Перемоги: %gp_wins%
Поразки: %gp_losses%
Winrate: %gp_winrate%%
```

### В скорборді (з плагіном скорборду):

```
&6&lGradient Pillars
&7Перемоги: &a%gp_wins%
&7Поразки: &c%gp_losses%
&7Winrate: &e%gp_winrate%%
```

### В TAB (з TAB плагіном):

```
Prefix: &7[%gp_wins%W]
```

## База даних

Статистика зберігається в MariaDB/MySQL базі даних.

### Налаштування БД (config.yml):

```yaml
database:
  type: mariadb
  host: localhost
  port: 3306
  database: gradientpillars
  username: root
  password: password
```

### Структура таблиці:

```sql
CREATE TABLE player_stats (
    uuid VARCHAR(36) PRIMARY KEY,
    player_name VARCHAR(16) NOT NULL,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)
```

## Кешування

Статистика кешується в пам'яті для швидкого доступу. Дані оновлюються в БД автоматично після кожної гри.
