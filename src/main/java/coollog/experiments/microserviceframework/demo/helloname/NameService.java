package coollog.experiments.microserviceframework.demo.helloname;

import coollog.experiments.microserviceframework.framework.Microservice;

public class NameService implements Microservice {

  private static String NAME = "Q Chen";

  public String getName() {
    return NAME;
  }
}
