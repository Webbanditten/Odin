FROM eclipse-temurin:11-jre
COPY ./dist /usr/src/app
WORKDIR /usr/src/app
EXPOSE 12322 12321
ENV TZ="Europe/Copenhagen"
ENTRYPOINT ["java", "-Xmx500m", "-Xms300m", "-jar", "Kepler-Server-all.jar"]
