FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/stats-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9090
CMD ["java", "-jar", "app.jar"]