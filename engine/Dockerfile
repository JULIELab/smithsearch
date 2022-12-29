# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
#RUN ./mvnw dependency:resolve
COPY src ./src
RUN ./mvnw clean package --settings .mvn/maven-settings.xml

FROM eclipse-temurin:17

WORKDIR /app
COPY --from=build /app/target/smithsearch-*.jar /app/smithsearch.jar
CMD ["java", "-jar", "/app/smithsearch.jar"]