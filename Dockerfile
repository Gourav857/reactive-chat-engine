# Stage 1: Build Phase (Maven aur Java 25 ka use karke jar file banayenge)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Sabse pehle pom.xml aur source code copy karenge
COPY pom.xml .
COPY src ./src

# Project ko compile karke executable JAR file build karenge (Tests ko skip kar rhe hain time bachane ke liye)
RUN mvn clean package -DskipTests

# Stage 2: Run Phase (Sirf lightweight JRE/JDK image use karenge taaki image size chota rhe)
FROM eclipse-temurin:21-jre
WORKDIR /app

# Stage 1 se bani hui JAR file ko uthakar is final image mein copy karenge
# Note: Apne pom.xml ke hisab se JAR ka naam check kar lena, general format yehi hota hai
COPY --from=build /app/target/*.jar app.jar

# Cloud platform ke dynamic port ko expose karenge
EXPOSE 8080

# Netty server ko launch karne ki command
ENTRYPOINT ["java", "-jar", "app.jar"]