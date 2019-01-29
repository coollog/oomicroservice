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

public class CollectorService extends Microservice {

  private long hits = 0;
  private long total = 0;

  public void start() {
    service(PiService.class).start();
  }

  public void hit() {
    hits++;
    total++;
    log();
  }

  public void miss() {
    total++;
    log();
  }

  private void log() {
    System.out.println("Pi=" + (hits * 4.0 / total));
  }
}
