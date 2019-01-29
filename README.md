OOMicroservice

Object-oriented microservice

8/19

- working on getting all files for running Java executable
- copied microserviceframework over
- helloname is working

10/11

- replacing Docker build with Jib Core

1/29

- getting it working again
- parallelize ServiceDeployer


Usage:

need kubectl with cluster context
need Docker
run 
```
./gradlew goJF run
kubectl get all
kubectl port-forward service/helloservice 8080:80
echo sayHello | nc localhost 8080
```


How it works:

- self-containerizes with Jib Core
- deploys to kubernetes
- mocks method calls into remote method invocations


Argument types supported:
- string
- integer
- class
Return types supported:
- string
- integer
- void