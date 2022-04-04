FROM openjdk:8-jdk-alpine
ARG JAR_FILE=target/sparrow-recsys-spring-boot-*.jar
ADD ${JAR_FILE} /app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m"
ENTRYPOINT ["java","-jar","/app.jar"]
#ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]
