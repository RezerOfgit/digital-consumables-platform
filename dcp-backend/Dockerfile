# 基础镜像：Eclipse Temurin JDK 17（openjdk 官方已停止维护）
FROM eclipse-temurin:17-jdk-jammy

LABEL maintainer="DCP-Developer"

WORKDIR /app

# 解决中文日志乱码
ENV LANG=C.UTF-8
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

# 复制 Maven 构建产物
COPY target/dcp-app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
