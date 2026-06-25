FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace

RUN apt-get update \
	&& apt-get install -y --no-install-recommends nodejs npm \
	&& rm -rf /var/lib/apt/lists/*

COPY pom.xml ./
COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

COPY --from=build /workspace/target/itip-web-backend-1.0.0-SNAPSHOT.jar /app/itip-web-backend.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/itip-web-backend.jar"]