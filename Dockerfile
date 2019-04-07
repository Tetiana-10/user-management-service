FROM java:8-jdk-alpine
COPY ./target/login-0.0.1-SNAPSHOT.jar /usr/app/
COPY ./ /usr/app/
WORKDIR /usr/app
RUN sh -c 'touch login-0.0.1-SNAPSHOT.jar'
ENTRYPOINT ["java","-jar","login-0.0.1-SNAPSHOT.jar"]
