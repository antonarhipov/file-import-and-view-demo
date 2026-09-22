# Amsterdam weather import demo

Run the application from this directory:

```shell
./mvnw spring-boot:run
```

Spring Boot starts the PostgreSQL service defined in `compose.yaml`, Flyway creates
the schema, and the application imports `data/amsterdam_weather.csv`. Re-running
the application is safe: PostgreSQL's `(name, observed_at)` unique constraint
ignores existing readings; the startup log reports the number skipped. Invalid
`name`, `datetime`, or `temp` values are logged and skipped without stopping the
application.

Open http://localhost:8080 to browse the readings. The page shows 50 results at
a time; use the Previous and Next links to paginate.

## Application flow

```mermaid
flowchart TD
    start[Spring Boot starts] --> compose[Docker Compose starts PostgreSQL]
    compose --> flyway[Flyway applies schema migrations]
    flyway --> importer[CSV importer reads amsterdam_weather.csv]
    importer --> parse{Valid name, datetime, and temp?}
    parse -- No --> invalid[Log row and skip it]
    parse -- Yes --> insert[Insert reading in PostgreSQL]
    insert --> duplicate{Existing name and datetime pair?}
    duplicate -- Yes --> skipped[Log duplicate summary and skip it]
    duplicate -- No --> stored[Store temperature reading]
    stored --> controller[Web controller queries readings]
    controller --> page[Paginated temperature UI]
    page --> browser[Browser]
```
