## InventAlert Identity Service 
InventAlert Identity Service Microservice

### MYSQL DB Setup
To avoid DB mismatch and issues, you can use docker to pull a
mysql image and run an instance.

```
docker --version
docker pull mysql
docker images
docker run --name identityService -p 3308:3306 -e MYSQL_ROOT_PASSWORD=password -d mysql:latest
docker ps
docker ps -a
docker exec -it mysql mysql -u root -p # To access the db in your terminal
docker exec -it broker bash # To access the kafka instance
```

