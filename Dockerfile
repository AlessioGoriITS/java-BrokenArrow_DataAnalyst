FROM maven:3.9.13-eclipse-temurin-21-noble AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode dependency:go-offline

COPY src ./src
COPY database ./database
RUN mvn --batch-mode clean verify

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system battle-debrief \
    && useradd --system --gid battle-debrief battle-debrief

WORKDIR /app

COPY --from=build --chown=battle-debrief:battle-debrief \
    /workspace/target/battle-debrief-0.0.1-SNAPSHOT.jar \
    /app/application.jar

USER battle-debrief

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
    CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
