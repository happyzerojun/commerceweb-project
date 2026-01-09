# 1단계: 빌드 스테이지
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# 현재 폴더(안쪽 backend)에 있는 Maven 재료들을 복사합니다
COPY pom.xml mvnw ./
COPY .mvn ./.mvn

# 의존성 내려받기
RUN ./mvnw dependency:go-offline -B

# 소스 복사 및 빌드
COPY src ./src
RUN ./mvnw package -DskipTests

# 2단계: 실행 스테이지
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]