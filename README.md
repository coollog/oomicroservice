Newer attempt at microservice fw

8/19

- working on getting all files for running Java executable
- copied microserviceframework over
- helloname is working

10/11

- replacing Docker build with Jib Core


Usage:

need kubectl with cluster context
need Docker
run 
```
./gradlew goJF run
kubectl get all
kubectl port-forward helloservice-* 8080:80
echo sayHello | nc localhost 8080
```
