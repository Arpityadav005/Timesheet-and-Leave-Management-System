FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY common/pom.xml common/pom.xml
COPY common/src common/src
RUN mvn -f common/pom.xml -DskipTests install

COPY leave-service/pom.xml leave-service/pom.xml
COPY leave-service/src leave-service/src
RUN mvn -f leave-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /workspace/leave-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
