# For the Docker image of the main API backend

FROM amazoncorretto:24-alpine3.22

ENV JAR_FILE_NAME=backend.jar

RUN mkdir -p /home/prod
COPY ./build/libs/$JAR_FILE_NAME /home/prod

CMD java -jar /home/prod/$JAR_FILE_NAME
