FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/personal-finance-api-*-SNAPSHOT.jar app.jar
RUN mkdir -p /var/data
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
