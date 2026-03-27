FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY common/pom.xml common/pom.xml
COPY common/src common/src
RUN mvn -f common/pom.xml -DskipTests install

COPY Config-Server/pom.xml Config-Server/pom.xml
COPY Config-Server/src Config-Server/src
RUN mvn -f Config-Server/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /workspace/Config-Server/target/*.jar app.jar

EXPOSE 7777

ENTRYPOINT ["java", "-jar", "app.jar"]
