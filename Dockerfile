
# Stage 1: Build
FROM maven:3.9.4-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && \
    apt-get install -y postgresql-client && \
    rm -rf /var/lib/apt/lists/*
COPY /docker/wait-for-postgreSQL.sh ./
RUN chmod +x wait-for-postgreSQL.sh
COPY --from=builder /app/target/sws-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["./wait-for-postgreSQL.sh"]
CMD ["java", \
     "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", \
     "-jar","app.jar"]
