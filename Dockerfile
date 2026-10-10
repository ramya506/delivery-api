# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS="-Xmx600m"
EXPOSE 8080
# Cloud Run sets PORT; application.properties reads it.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
