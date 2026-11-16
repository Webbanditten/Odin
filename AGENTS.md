# Kepler Server - Development Notes

## Build Instructions

### Environment
- **Language:** Java 21 (Temurin/OpenJDK 21 LTS). The Gradle toolchain is pinned to 21.
- **Build tool:** Gradle 9.9.1 via the wrapper (`./gradlew`). Never use a system `gradle`.
- **IDE:** IntelliJ IDEA (delegated build disabled, so IntelliJ uses its own compiler). Set the Project SDK to 21.

### Building
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home)
./gradlew fatJar
```
Output: `Kepler-Server/build/libs/Kepler-Server-all.jar`.

Headless/Docker alternative:
```bash
docker run --rm -v "$(pwd):/project" -w /project eclipse-temurin:21-jdk ./gradlew fatJar
```

### Running locally (macOS / Apple Silicon works)
```bash
docker compose -f docker/docker-compose.dev.yml up -d   # MariaDB 10.4.18 + RabbitMQ, bound to 127.0.0.1
# seed: gunzip -c <prod dump>.sql.gz | docker exec -i odin-dev-mariadb mysql -uroot -podin
cp server.ini.example server.ini                         # gitignored; matches the dev compose credentials
$JAVA_HOME/bin/java -jar Kepler-Server/build/libs/Kepler-Server-all.jar
```
Run it from the repo root so the server finds `server.ini` and `tools/snowstorm_maps`. Logs go to stdout,
`server.log` and `error.log`.

### Common Build Issues

| Issue | Cause | Fix |
|-------|-------|-----|
| `Unsupported class file major version` / Gradle fails to start | Wrong JDK on `JAVA_HOME` | Point `JAVA_HOME` at JDK 21 |
| lazysodium `FileAlreadyExistsException ... META-INF/license` | Case-insensitive FS collision in the fat jar | Already excluded in `fatJar`. Keep the `META-INF/license/**` exclude |
| `gradle compileJava` fails locally | System Gradle version incompatible | Use the wrapper (`./gradlew`) not system `gradle` |

## Project Structure

```
Kepler/
├── Kepler-Server/          # Main server source
│   ├── build.gradle        # Module build config
│   └── src/main/java/org/alexdev/kepler/
│       ├── Kepler.java     # Entry point
│       ├── dao/mysql/       # Database access objects (DAO pattern)
│       ├── game/            # Game logic (rooms, players, moderation, etc.)
│       ├── messages/        # Packet handlers (incoming/) and composers (outgoing/)
│       └── util/            # Utilities (DateUtil, etc.)
├── tools/                   # SQL schemas and migrations
│   ├── kepler.sql           # Main database schema
│   └── migrations/          # Incremental SQL migrations
├── dist/                    # Pre-built JAR for Docker
├── Dockerfile              # Runtime container (eclipse-temurin:21-jre, runs from dist/)
└── settings.gradle         # Root Gradle settings
```

## Database

- **Engine:** MariaDB/MySQL
- **Connection pool:** HikariCP
- **DAO pattern:** Static methods, manual connection management with try/finally

Example DAO pattern:
```java
public static void doSomething(int param) {
    Connection sqlConnection = null;
    PreparedStatement preparedStatement = null;
    try {
        sqlConnection = Storage.getStorage().getConnection();
        preparedStatement = Storage.getStorage().prepare("SQL HERE", sqlConnection);
        preparedStatement.setInt(1, param);
        preparedStatement.execute();
    } catch (Exception e) {
        Storage.logError(e);
    } finally {
        Storage.closeSilently(preparedStatement);
        Storage.closeSilently(sqlConnection);
    }
}
```

## Docker Deployment

The production Docker image runs from a pre-built JAR:
```dockerfile
FROM eclipse-temurin:21-jre
COPY ./dist /usr/src/app
WORKDIR /usr/src/app
EXPOSE 12322 12321
ENTRYPOINT ["java", "-Xmx500m", "-Xms300m", "-jar", "Kepler-Server-all.jar"]
```

CI builds the fat JAR, copies to `dist/`, then builds and pushes the Docker image.
