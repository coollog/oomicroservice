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

package coollog.experiments.microserviceframework;

import com.google.cloud.tools.jib.configuration.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.image.InvalidImageReferenceException;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import coollog.experiments.microserviceframework.deployer.Deployer;
import coollog.experiments.microserviceframework.packager.ClasspathResolver;
import coollog.experiments.microserviceframework.packager.ContainerBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** THIS IS JUST FOR TESTING */
public class SomeClass {

  public static void main(String[] args)
      throws InterruptedException, IOException, ExecutionException, InvalidImageReferenceException,
          CacheDirectoryCreationException {
    if (args.length > 0) {
      helloWorld();
      return;
    }

    // Gets all the files to package.
    List<Path> classpathFiles = ClasspathResolver.getClasspathFiles();
    classpathFiles.forEach(System.out::println);

    // Packages the files into a container.
    String imageReference = "gcr.io/qingyangc-sandbox/helloworld";
    ContainerBuilder.containerize(
        classpathFiles, imageReference, "coollog.experiments.microserviceframework.SomeClass", "");

    // Runs the container on kubernetes.
    Deployer.deploy("helloworld", imageReference);
  }

  private static void helloWorld() {
    ImmutableList<String> someList = ImmutableList.of("hello", "world");
    System.out.println(Joiner.on(" ").join(someList));
  }
}
