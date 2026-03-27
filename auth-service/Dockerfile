FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY common/pom.xml common/pom.xml
COPY common/src common/src
RUN mvn -f common/pom.xml -DskipTests install

COPY auth-service/pom.xml auth-service/pom.xml
COPY auth-service/src auth-service/src
RUN mvn -f auth-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /workspace/auth-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
