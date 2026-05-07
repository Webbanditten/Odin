# Kepler Server - Development Notes

## Build Instructions

### Environment
- **Language:** Java 11 (AdoptOpenJDK/Temurin 11)
- **Build tool:** Gradle 7.2 (via wrapper `gradlew`)
- **IDE:** IntelliJ IDEA (delegated build disabled — uses IntelliJ's own compiler)

### Building Locally

The `gradlew` script has Windows line endings (`\r\n`) which breaks under WSL/Linux shells. 

**Option 1: Build via Docker (recommended for CI/headless)**
```bash
docker run --rm -v "$(pwd):/project" -w /project adoptopenjdk:11-jdk-hotspot sh -c "sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew compileJava"
```

**Option 2: Build a fat JAR (for deployment)**
```bash
docker run --rm -v "$(pwd):/project" -w /project adoptopenjdk:11-jdk-hotspot sh -c "sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew fatJar"
```
Output: `Kepler-Server/build/libs/Kepler-Server-all.jar`

**Option 3: IntelliJ IDEA**
- Open the project in IntelliJ
- Ensure Project SDK is set to Java 11
- IntelliJ handles the build automatically (gradle.xml has `delegatedBuild=false`)

### Common Build Issues

| Issue | Cause | Fix |
|-------|-------|-----|
| `Unsupported class file major version 68` | System Gradle using wrong JDK (e.g. Java 24) | Use Docker build or set `JAVA_HOME` to JDK 11 |
| `/usr/bin/env: 'sh\r': No such file or directory` | Windows line endings in `gradlew` | Run `sed -i 's/\r$//' gradlew` before executing |
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
├── Dockerfile              # Runtime container (openjdk:12, runs from dist/)
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
FROM openjdk:12
COPY ./dist /usr/src/app
WORKDIR /usr/src/app
EXPOSE 12322 12321
ENTRYPOINT ["java", "-Xmx500m", "-Xms300m", "-jar", "Kepler-Server-all.jar"]
```

CI builds the fat JAR, copies to `dist/`, then builds and pushes the Docker image.
