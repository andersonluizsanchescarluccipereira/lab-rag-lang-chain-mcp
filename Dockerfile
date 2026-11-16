FROM docker.io/library/maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM docker.io/library/eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r app && useradd --no-log-init -r -g app app
COPY --from=build --chown=app:app /app/target/pix-chave-java21-mvc-jwt-1.0.0.jar /app/app.jar
USER app
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
