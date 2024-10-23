FROM maven:3.8.7-eclipse-temurin-17
WORKDIR /usr/src/app
COPY . .
RUN apt-get update && apt-get install -y allure
RUN mvn clean package -DskipTests
CMD ["sh", "-c", "mvn clean test && allure generate --clean target/allure-results"]
