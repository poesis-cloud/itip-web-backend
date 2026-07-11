FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

COPY --from=build /workspace/target/itip-web-backend-1.0.0-SNAPSHOT.jar /app/itip-web-backend.jar
ENV ENABLE_TEST_SUPERADMIN_SEED=false

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Denable.test.superadmin.seed=${ENABLE_TEST_SUPERADMIN_SEED} -jar /app/itip-web-backend.jar"]