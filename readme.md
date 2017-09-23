
amv-access-api
========

## Development

### Spring Boot
```bash
$ ./gradlew bootRun
```
### Build & Run
```bash
$ ./gradlew build && java -jar build/libs/amv-access-api-<version>.jar
 --spring.profiles.active=production,debug
```

```bash
$ curl localhost:9000/manage/health
{"status":"UP","diskSpace":{"status":"UP","total":397635555328,"free":328389529600,"threshold":10485760}}}
```

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


### Deploy
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