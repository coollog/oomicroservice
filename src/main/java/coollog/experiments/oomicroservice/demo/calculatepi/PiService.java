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

import coollog.experiments.oomicroservice.framework.Microservice;

public class PiService extends Microservice {

  public void start() {
    new Thread(this::pi).start();
  }

  @SuppressWarnings("InfiniteLoopStatement")
  private void pi() {
    while (true) {
      double x = Math.random();
      double y = Math.random();
      if (Math.sqrt(x * x + y * y) < 1.0) {
        service(CollectorService.class).hit();
      } else {
        service(CollectorService.class).miss();
      }
    }
  }
}
