FROM openjdk:8-jdk-alpine

EXPOSE 9090

ARG JAR_FILE

ADD ${JAR_FILE} /opt/youtube-channel/app.jar

ENTRYPOINT ["java","-jar","/opt/youtube-channel/app.jar","-Dspring.profiles.active=openshift"]