# ---- Build stage ----
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY common-lib/pom.xml common-lib/
COPY common-lib/src common-lib/src/
COPY discovery-service/pom.xml discovery-service/
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY catalog-service/pom.xml catalog-service/
COPY inventory-service/pom.xml inventory-service/
COPY order-service/pom.xml order-service/
COPY order-processing-service/pom.xml order-processing-service/
COPY aggregator-service/pom.xml aggregator-service/

RUN mvn dependency:go-offline -pl common-lib,catalog-service --also-make -q

COPY catalog-service/src catalog-service/src/
RUN mvn install -N -q -DskipTests
RUN mvn install -pl common-lib -q -DskipTests
RUN mvn package -pl catalog-service -q -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/catalog-service/target/*.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8083/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
