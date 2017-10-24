[![Build Status](https://travis-ci.org/amvnetworks/amv-access-api-poc.svg?branch=master)](https://travis-ci.org/amvnetworks/amv-access-api-poc)
[![Download](https://api.bintray.com/packages/amvnetworks/amv-access-api-poc/client/images/download.svg) ](https://bintray.com/amvnetworks/amv-access-api-poc/client/_latestVersion)
[![License](https://img.shields.io/github/license/amvnetworks/amv-access-api-poc.svg?maxAge=2592000)](https://github.com/amvnetworks/amv-access-api-poc/blob/master/LICENSE)

amv-access-api
========
This is a proof of concept application.
amv-access-api requires Java version 1.8 or greater.

# Client
## Setup
### gradle
```groovy
compile 'org.amv.access:client:${amvAccessVersion}'
```
### maven
```xml
<dependency>
  <groupId>org.amv.access</groupId>
  <artifactId>client</artifactId>
  <version>${amvAccessVersion}</version>
</dependency>
```

## Device Certificate Client
```java
String baseUrl = "https://www.example.com";
DeviceCertClient deviceCertClient = Clients.deviceCertClient(Clients.simpleFeign(), baseUrl);
```
### Create Device Certificates
```
String apiKey = "...";
String publicKeyBase64 = "...";

CreateDeviceCertificateRequestDto body = CreateDeviceCertificateRequestDto.builder()
        .devicePublicKey(publicKeyBase64)
        .build();

CreateDeviceCertificateResponseDto response = deviceCertClient
        .createDeviceCertificate(apiKey, body)
        .execute();
```

# Build
Build a snapshot from a clean working directory
```bash
$ ./gradlew releaseCheck clean build -Prelease.stage=SNAPSHOT -Prelease.scope=patch
```

When a parameter `minimal` is provided, certain tasks will be skipped to make the build faster.
e.g. `findbugs`, `checkstyle`, `javadoc` - tasks which results are not essential for a working build.
```bash
./gradlew clean build -Pminimal
```

## create a release
```bash
./gradlew final -Prelease.scope=patch
```

## release to bintray
```bash
./gradlew clean build final bintrayUpload
  -Prelease.useLastTag=true
  -PreleaseToBintray
  -PbintrayUser=${username}
  -PbintrayApiKey=${apiKey}
```

## Development
### Spring Boot
Run the application with active `development` profile
```bash
$ ./gradlew web:bootRun -Dspring.profiles.active=development
```

### Build & Run
Building and running the final jar
```bash
$ ./gradlew clean build -Pminimal && java -jar web/build/libs/amv-access-api-web-<version>.jar
 --spring.profiles.active=production,debug
```
Check application is up and running
```bash
$ curl localhost:9001/manage/health
{"status":"UP","diskSpace":{"status":"UP","total":397635555328,"free":328389529600,"threshold":10485760}}}
```

### IDE
As this project uses [Project Lombok](https://projectlombok.org/) make sure you have annotation processing enabled.

### Swagger
Open `http://localhost:9000/amv-access-api/swagger-ui.html` in your browser.

### Metrics
Prometheus is supported as monitoring system. Metrics can be pulled from `http://localhost:9001/manage/prometheus` 

### Docker
#### Build
```bash
$ docker build -t amv/amv-access-api .
```
#### Run
```bash
$ docker run -t -i -p 9000:9000 amv/amv-access-api
```
or
```bash
$ docker-compose up
```


## Deploy
```bash
$ cp web/build/libs/amv-access-api-<version>.jar /var/amv/amv-access-api/amv-access-api.jar
$ cp deploy/amv-access-api.conf /var/amv/amv-access-api/amv-access-api.conf
$ cp deploy/amv-access-api.service /etc/systemd/system
```

To flag the application to start automatically on system boot use the following command:
```bash
$ systemctl enable amv-access-api.service
```

To start/stop the application manually use the following command:
```bash
$ systemctl start amv-access-api
$ systemctl stop amv-access-api
$ systemctl status amv-access-api
```

# license
The project is licensed under the Apache License. See [LICENSE](LICENSE) for details.
