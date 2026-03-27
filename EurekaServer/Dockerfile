FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY EurekaServer/pom.xml EurekaServer/pom.xml
COPY EurekaServer/src EurekaServer/src
RUN mvn -f EurekaServer/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /workspace/EurekaServer/target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
