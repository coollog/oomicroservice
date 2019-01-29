/*
 * Copyright 2018 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package coollog.experiments.oomicroservice.demo.helloname;

import coollog.experiments.oomicroservice.framework.ServiceDeployer;
import coollog.experiments.oomicroservice.framework.ServiceRunner;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

/**
 * Demo main class.
 *
 * <p>When run without any args, the demo runs as a deployer and deploys all the microservices to
 * Kubernetes.
 *
 * <p>When run with an arg (as a container on Kubernetes), that arg is treated as the name of the
 * microservice to run.
 */
public class Runner extends ServiceRunner {

  public static void main(String[] args)
      throws ClassNotFoundException, IOException, InstantiationException, InvocationTargetException,
          InterruptedException, ExecutionException {
    // Registers the microservices.
    register(NameService.class);
    register(HelloService.class);

    // When no args, runs as a deployer.
    if (args.length == 0) {
      ServiceDeployer.deploy(Runner.class);
      return;
    }

    // When there is an arg, runs as the corresponding microservice.
    String runClassName = args[0];
    run(runClassName);
  }
}
