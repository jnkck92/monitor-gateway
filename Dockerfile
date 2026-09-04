FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY target/monitor-gateway-*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]