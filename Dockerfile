FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
RUN mkdir -p /app/media-storage
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
