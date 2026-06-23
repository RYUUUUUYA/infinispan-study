FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV PORT=8080
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/target/infinispan-demo-1.0.jar /app/app.jar
EXPOSE 8080 7800
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD ["sh", "-c", "curl -fsS http://localhost:${PORT}/health >/dev/null || exit 1"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
