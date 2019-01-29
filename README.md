![experimental](https://img.shields.io/badge/stability-experimental-red.svg)
[![Gitter chat](https://badges.gitter.im/coollog/oomicroservice.png)](https://gitter.im/coollog/oomicroservice)

# OOMicroservice (Object-oriented microservices)

**Note: This is an experimental prototype. Do NOT use for production.**

## What is this?

Writing microservices tends to involve a lot of boilerplate and set-up for each microservice. What if you could **write a distributed application as if it were a local application?**

OOMicroservice is an experiment to allow you to write normal Java classes and automatically run them as microservices on a Kubernetes cluster. You can use the usual object-oriented constructs to build the microservices. Over-the-network calls between microservices are simply local method calls that are also type-completed. The application also containerizes and deploys itself to Kubernetes.

### Simple example

Let's say we want two microservices, `HelloService` and `NameService`. `NameService` serves your name via a `getName` endpoint. `HelloService` calls `NameService`'s `getName` endpoint and serves a greeting.

To implement this with OOMicroservice, you would simply have two classes:

`NameService.java`:

```java
public class NameService extends Microservice {
  public String getName() {
    return "Serverless Fan";
  }
}
``` 

`HelloService.java`:

```java
public class HelloService extends Microservice {
  public String sayHello() {
    return "Hello, " + service(NameService.class).getName();
  }
}
```

Then a main class `Runner` just registers the two microservices and runs.

`Runner.java`:

```java
public class Runner extends ServiceRunner {
  public static void main(String[] args) throws Exception {
    register(NameService.class);
    register(HelloService.class);

    runMain(Runner.class, args);
  }
}
```

## Try it out

### Prerequisites

- `kubectl` configured with a Kubernetes cluster ([like GKE](https://cloud.google.com/kubernetes-engine/docs/quickstart))

### Setup

```bash
git clone && cd oomicroservice
```

In `build.gradle`, change `IMAGE_PREFIX` to your own Docker registry prefix (include the trailing slash).

### Helloname Demo

In a terminal, run:

```bash
$ ./gradlew runHellonameDemo
```

This will run the [Helloname demo](src/main/java/coollog/experiments/oomicroservice/demo/helloname), which will containerize and deploy itself to your Kubernetes cluster.

Port-forward `HelloService` so that you can call the `sayHello` method:

```bash
$ kubectl port-forward service/helloservice 8080:80
```

In another terminal window, call the `sayHello` method:

```bash
$ echo sayHello | nc localhost 8080
Hello, Serverless Fan
```

### PI Calculator Demo

This demo calculates PI using a probabilistic method. `PiService` runs the probabilistic trials and `CollectorService` collects the trial results into an estimate for PI. The longer the services run, the closer the run should be to the actual value of PI.

In a terminal, run:

```bash
$ ./gradlew runPiDemo
```

This will run the [Pi calculator demo](src/main/java/coollog/experiments/oomicroservice/demo/calculatepi), which will containerize and deploy itself to your Kubernetes cluster.

Port-forward the `CollectorService` so that you can call the `start` method to start the trials:

```bash
$ kubectl port-forward service/collectorservice 8080:80
```

In another terminal window, call the `start` method:

```bash
$ echo start | nc localhost 8080
```

Then, watch the collector's logs to see the results:

```bash
$ kubectl logs -f deploy/collectorservice
```

#### Try inheritance

Add another class that also runs the same trials as `PiService`. Make a `PiService2.java`:

```java
public class PiService2 extends PiService {}
```

This `PiService2` will inherit the behavior of `PiService`.

Have `CollectorService` also start trials on this `PiService2` by adding it to `CollectorService#start`:

```java
public class CollectorService extends Microservice {
  ...
  public void start() {
    service(PiService.class).start();
    service(PiService2.class).start();
  }
  ...
}
```

Register `PiService2` in `Runner.java`:

```java
public class Runner extends ServiceRunner {

  public static void main(String[] args) {
    register(CollectorService.class);
    register(PiService.class);
    register(PiService2.class);

    runMain(Runner.class, args);
  }
}
```

Re-run the demo and now `CollectorService` is collecting trial results from both `PiService` and `PiService2`.

### Message queue (PubSub)

*To be added*

## How it works

OOMicroservice converts regular classes into microservices. It uses [Jib Core](https://github.com/GoogleContainerTools/jib/tree/master/jib-core) to self-containerize and `kubectl` to self-deploy to Kubernetes as microservices. The core of the library essentially mocks any local method calls into remote method calls so that invoking a method on a class turns into a over-the-network call to another Kubernetes service.

#### Current limitations

This project is just an experiment at this point, so things may just break. Some current limitations include:

- Ssingletons only
- No shared memory between microservices
- Volatile state
- Insecure method serialization

For method invocations, there are only a few argument types supported:

- String
- Integer
- Class

And only a few return types supported:

- String
- Integer
- Void

## Updates

1/29

- Set up the public repository
