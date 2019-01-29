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

package coollog.experiments.oomicroservice.framework;

import com.google.cloud.tools.jib.configuration.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.image.DescriptorDigest;
import com.google.cloud.tools.jib.image.ImageReference;
import com.google.cloud.tools.jib.image.InvalidImageReferenceException;
import com.google.common.base.Verify;
import coollog.experiments.oomicroservice.framework.deployer.KubectlDeployer;
import coollog.experiments.oomicroservice.packager.ClasspathResolver;
import coollog.experiments.oomicroservice.packager.ContainerBuilder;
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

/** Containerizes and deploys services to Kubernetes. */
public class ServiceDeployer {

  /**
   * Set the {@code IMAGE_PREFIX} environment variable to the image repository you would like to use
   * to store the container images.
   */
  private static final String IMAGE_REPOSITORY;

  static {
    IMAGE_REPOSITORY = System.getenv("IMAGE_PREFIX");
    if (IMAGE_REPOSITORY == null) {
      throw new IllegalStateException("Must define IMAGE_PREFIX environment variable");
    }
  }

  /**
   * Containerizes and deploys the services to Kubernetes.
   *
   * @param mainClass the main class to run the services with. This main class is run with the name
   *     of the {@link Microservice} class to serve.
   * @throws InterruptedException if the deployment is interrupted
   * @throws ExecutionException if the deployment execution throws an exception
   */
  public static void deploy(Class<?> mainClass) throws InterruptedException, ExecutionException {
    // Gets the registered services to deploy.
    List<ServiceRegistry.RegisteredMicroservice<?>> registeredMicroservices =
        ServiceRegistry.getRegisteredMicroservices();

    ServiceDeployer serviceDeployer = new ServiceDeployer(mainClass.getName());
    for (ServiceRegistry.RegisteredMicroservice<?> registeredMicroservice :
        registeredMicroservices) {
      // Queues up the deployment pipeline for each service. Each service is deployed to run
      serviceDeployer.addDeployment(
          IMAGE_REPOSITORY, registeredMicroservice.getClazz(), registeredMicroservice.getHost());
    }
    serviceDeployer.deployAll();
  }

  private final ExecutorService executorService = Executors.newCachedThreadPool();
  private final String mainClass;
  private final List<Callable<Void>> deploymentCallables = new ArrayList<>();

  private ServiceDeployer(String mainClass) {
    this.mainClass = mainClass;
  }

  private void addDeployment(
      String imageRepository, Class<? extends Microservice> clazz, String host) {
    deploymentCallables.add(
        () -> {
          deploy(imageRepository, clazz, host);
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

  /**
   * Containerizes the microservice with image reference prefix {@code imageRepository} and deploys
   * to Kubernetes.
   *
   * @param imageRepository the image reference prefix (including slash)
   * @param clazz the {@link Microservice} class
   * @param host the hostname to serve the microservice as
   * @throws IOException if an I/O exception occurs
   * @throws InvalidImageReferenceException if the generated image reference is invalid
   * @throws InterruptedException if the deployment is interrupted
   * @throws ExecutionException if the deployment execution throws an exception
   * @throws CacheDirectoryCreationException if the Jib cache directory could not be created
   */
  private void deploy(String imageRepository, Class<? extends Microservice> clazz, String host)
      throws IOException, InvalidImageReferenceException, InterruptedException, ExecutionException,
          CacheDirectoryCreationException {
    // Gets all the files to package.
    List<Path> classpathFiles = ClasspathResolver.getClasspathFiles();

    // Packages the files into a container.
    String imageReference = imageRepository + host;
    System.out.println("Containerizing " + imageReference);
    DescriptorDigest containerDigest =
        ContainerBuilder.containerize(
            classpathFiles, imageReference, mainClass, clazz.getName(), executorService);
    System.out.println("Containerized " + imageReference);

    // Runs the container on kubernetes.
    ImageReference imageReferenceWithDigest =
        ImageReference.parse(imageReference).withTag(containerDigest.toString());
    System.out.println("Deploying " + imageReferenceWithDigest);
    KubectlDeployer.deploy(host, imageReferenceWithDigest.toString());
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
