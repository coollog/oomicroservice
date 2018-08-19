package coollog.experiments.microserviceframework.demo.helloname;

import coollog.experiments.microserviceframework.framework.Microservice;
import coollog.experiments.microserviceframework.framework.Services;

public class HelloService implements Microservice {

  public String sayHello() {
    String name = Services.get(NameService.class).getName();
    return "Hello, " + name;
  }
}
