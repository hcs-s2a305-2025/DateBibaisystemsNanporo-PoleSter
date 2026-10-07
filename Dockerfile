FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 手元のPCでビルドされた JAR ファイルをコピー
COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]