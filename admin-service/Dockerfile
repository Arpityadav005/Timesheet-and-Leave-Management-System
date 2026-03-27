FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY common/pom.xml common/pom.xml
COPY common/src common/src
RUN mvn -f common/pom.xml -DskipTests install

COPY admin-service/pom.xml admin-service/pom.xml
COPY admin-service/src admin-service/src
RUN mvn -f admin-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /workspace/admin-service/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
