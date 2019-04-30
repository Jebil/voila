# voila
Basic application with micro service architecture built on Spring  cloud and docker


Heimdall - Gateway service
Eureka - Service discovery

## Requirements

* Java OpenJDK 11

## Java

### Gradle

Important Gradle tasks

#### Start / stop DB

Starts Docker Compose application with PostgreSQL running.
Useful when running the app from IDE during development and testing:

    ./gradlew dbUp
    
and to stop it:

    ./gradlew dbDown    

### Start / stop DB + all services

    ./gradles allUp
    
and

    ./gradlew allDown    
    
### Delete DB and restart all services

    ./gradlew allDown dbDelete allUp    

#### Run Heimdall or Eureka Server application

    ./gradlew heimdall-server:bootRun
    ./gradlew eureka-server:bootRun

#### Run Eureka Server & PostgreSQL together

Starts up Docker Compose application with Eureka & PostgreSQL as one app.
Useful for running automated integration tests from Gradle / Jenkins.
    
    ./gradlew bootJar docker eureka-server:dockerUp
   
and

     ./gradlew eureka-server:dockerDown
     
and to view logs of the Eureka server Docker container

    ./gradlew eureka-server:dockerLogs         

#### Cucumber BDD integration tests

All you need is

    ./gradlew clean build integTest

#### Code format

Formats all Java code in accordance to the [Google Java style](https://google.github.io/styleguide/javaguide.html):

    ./gradlew format
