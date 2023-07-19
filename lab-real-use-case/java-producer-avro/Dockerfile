FROM gradle:7.4.2-jdk11-focal AS builder
WORKDIR /home/gradle/project
COPY build.gradle ./
COPY src ./src
USER root
RUN gradle build

FROM openjdk:11.0.16-jre-slim
RUN apt-get update && \
apt-get install -y curl
COPY --from=builder /home/gradle/project/build/distributions/project.tar /app/
COPY data /app/project/data
WORKDIR /app
RUN tar -xvf project.tar
WORKDIR /app/project
CMD java -classpath "lib/*" clients.Producer
        