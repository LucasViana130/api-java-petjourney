FROM maven:3.9.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S petjourney && adduser -S petjourney -G petjourney

COPY --from=build /app/target/petjourney-0.0.1-SNAPSHOT.jar app.jar

USER petjourney

ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=64m -XX:+UseSerialGC -Xss512k -Djava.net.preferIPv4Stack=true -Dspring.jmx.enabled=false"

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
