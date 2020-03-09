# Dockerfile for test-server

FROM gcr.io/distroless/java:11
MAINTAINER Jebil Kuruvila <jebil4u@gmail.com>

# Docker best practices, used to force a rebuild of the entire filesystem
ENV REFRESHED_AT 2019-04-29

# builds from the Docker context created by the Gradle Docker plugin
COPY libs/video-streamer-*.jar /opt/jk/video-streamer.jar
COPY bootstrap.yml /opt/jk/config/bootstrap.yml

# expose server ports
EXPOSE 4444 4444

# distroless doesn't have curl so remove this for now
# define health check for best integration with Docker Compose
# HEALTHCHECK CMD curl --fail http://localhost:8080/ || exit 1

# Run Spring Boot app
WORKDIR /opt/jk/
CMD ["video-streamer.jar"]



