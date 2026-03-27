FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY common/pom.xml common/pom.xml
COPY common/src common/src
RUN mvn -f common/pom.xml -DskipTests install

COPY timesheet-service/pom.xml timesheet-service/pom.xml
COPY timesheet-service/src timesheet-service/src
RUN mvn -f timesheet-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /workspace/timesheet-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
