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

package coollog.experiments.microserviceframework.framework;

import com.google.cloud.tools.jib.configuration.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.image.DescriptorDigest;
import com.google.cloud.tools.jib.image.ImageReference;
import com.google.cloud.tools.jib.image.InvalidImageReferenceException;
import com.google.common.base.Verify;
import coollog.experiments.microserviceframework.deployer.Deployer;
import coollog.experiments.microserviceframework.packager.ClasspathResolver;
import coollog.experiments.microserviceframework.packager.ContainerBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** VERY MUCH EXPERIMENTAL */
public class ServiceDeployer {

  public static void redeploy(String runnerClass) throws InterruptedException, ExecutionException {
    List<MicroserviceMethodHandler<?>> microserviceMethodHandlers =
        Services.getServiceMethodHandlers();

    ServiceDeployer serviceDeployer = new ServiceDeployer(runnerClass);
    for (MicroserviceMethodHandler<?> microserviceMethodHandler : microserviceMethodHandlers) {
      serviceDeployer.addDeployment(microserviceMethodHandler);
    }
    serviceDeployer.deployAll();
  }

  private final ExecutorService executorService = Executors.newCachedThreadPool();
  private final String runnerClass;
  private final List<Callable<Void>> deploymentCallables = new ArrayList<>();

  private ServiceDeployer(String runnerClass) {
    this.runnerClass = runnerClass;
  }

  private void addDeployment(MicroserviceMethodHandler<?> microserviceMethodHandler) {
    deploymentCallables.add(
        () -> {
          deploy(microserviceMethodHandler);
          return null;
        });
  }

  private void deployAll() throws ExecutionException, InterruptedException {
    List<Future<Void>> futures = executorService.invokeAll(deploymentCallables);
    for (Future<Void> future : futures) {
      Verify.verify(future.isDone());
      future.get();
    }
    shutdown();
  }

  private void deploy(MicroserviceMethodHandler<?> microserviceMethodHandler)
      throws IOException, InvalidImageReferenceException, InterruptedException, ExecutionException,
          CacheDirectoryCreationException {
    // Gets all the files to package.
    List<Path> classpathFiles = ClasspathResolver.getClasspathFiles();
    //    classpathFiles.forEach(System.out::println);

    // Packages the files into a container.
    String imageReference = "gcr.io/qingyangc-sandbox/" + microserviceMethodHandler.getHost();
    System.out.println("Containerizing " + imageReference);
    DescriptorDigest containerDigest =
        ContainerBuilder.containerize(
            classpathFiles,
            imageReference,
            runnerClass,
            microserviceMethodHandler.getClazz().getName(),
            executorService);
    System.out.println("Containerized " + imageReference);

    // Runs the container on kubernetes.
    ImageReference imageReferenceWithDigest =
        ImageReference.parse(imageReference).withTag(containerDigest.toString());
    System.out.println("Deploying " + imageReferenceWithDigest);
    Deployer.deploy(microserviceMethodHandler.getHost(), imageReferenceWithDigest.toString());
    System.out.println("Deployed " + imageReferenceWithDigest);
  }

  private void shutdown() {
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(100, TimeUnit.MICROSECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException ex) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
