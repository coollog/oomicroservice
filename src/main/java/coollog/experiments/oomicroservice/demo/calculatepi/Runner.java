/*
 * Copyright 2019 Google LLC. All rights reserved.
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

package coollog.experiments.oomicroservice.demo.calculatepi;

import coollog.experiments.oomicroservice.framework.ServiceRunner;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

public class Runner extends ServiceRunner {

  public static void main(String[] args)
      throws ClassNotFoundException, IOException, InstantiationException, InvocationTargetException,
          InterruptedException, ExecutionException {
    // Registers the microservices.
    register(CollectorService.class);
    register(PiService.class);

    runMain(Runner.class, args);
  }
}
