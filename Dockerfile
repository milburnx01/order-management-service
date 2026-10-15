FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY order-service/pom.xml order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

RUN mvn -B -ntp dependency:go-offline -DskipTests || true

COPY order-service/src order-service/src
COPY inventory-service/src inventory-service/src
COPY notification-service/src notification-service/src

ARG SERVICE

RUN test -n "$SERVICE" && \
    mvn -B -ntp -pl "$SERVICE" -am package -DskipTests && \
    find "$SERVICE/target" -maxdepth 1 -type f \
    -name '*.jar' \
    ! -name '*.jar.original' \
    ! -name '*-sources.jar' \
    ! -name '*-javadoc.jar' \
    -exec cp {} /workspace/app.jar \;

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/app.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]