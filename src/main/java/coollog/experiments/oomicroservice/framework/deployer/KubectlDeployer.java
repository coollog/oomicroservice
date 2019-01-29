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

package coollog.experiments.oomicroservice.framework.deployer;

import coollog.experiments.oomicroservice.command.Command;
import java.io.IOException;

/** Deploys a container image as a microservice using {@code kubectl}. */
public class KubectlDeployer {

  public static void deploy(String serviceName, String imageReference)
      throws IOException, InterruptedException {
    try {
      Command.runCommand("kubectl", "delete", "service,deployment", serviceName);

    } catch (IOException ex) {
      // Ignores any exceptions.
    }
    Command.runCommand("kubectl", "run", serviceName, "--image", imageReference);
    Command.runCommand(
        "kubectl", "expose", "deployment", serviceName, "--port=80", "--target-port=80");
  }

  private KubectlDeployer() {}
}
