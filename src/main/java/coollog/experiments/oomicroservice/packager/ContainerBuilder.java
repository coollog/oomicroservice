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

package coollog.experiments.oomicroservice.packager;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.configuration.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.filesystem.AbsoluteUnixPath;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;
import com.google.cloud.tools.jib.image.DescriptorDigest;
import com.google.cloud.tools.jib.image.ImageReference;
import com.google.cloud.tools.jib.image.InvalidImageReferenceException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/** Builds a container image */
public class ContainerBuilder {

  /** Containerizes using Jib Core. */
  private static class JibContainerizer {

    /**
     * Containerizes a Java container image that runs {@code classpathFiles}.
     *
     * @param classpathFiles the classpath files
     * @param imageReference the image reference to containerize to
     * @param mainClass the main class to run
     * @param arg the argument to pass to the main class
     * @param executorService the {@link ExecutorService} to run the containerization with
     * @return the digest of the built container image
     * @throws InvalidImageReferenceException if the image reference is invalid
     * @throws IOException if an I/O exception occurs
     * @throws InterruptedException if the execution is interrupted
     * @throws ExecutionException if an exception occurs during containerization
     * @throws CacheDirectoryCreationException if the Jib cache failed to create
     */
    private static DescriptorDigest containerize(
        List<Path> classpathFiles,
        String imageReference,
        String mainClass,
        String arg,
        ExecutorService executorService)
        throws InvalidImageReferenceException, IOException, InterruptedException,
            ExecutionException, CacheDirectoryCreationException {
      ImageReference targetImageReference = ImageReference.parse(imageReference);
      return Jib.from("gcr.io/distroless/java")
          .addLayer(classpathFiles, AbsoluteUnixPath.get("/app"))
          .setEntrypoint(Arrays.asList("java", "-cp", "/app/:/app/*", mainClass, arg))
          .containerize(
              Containerizer.to(
                      RegistryImage.named(targetImageReference)
                          .addCredentialRetriever(
                              CredentialRetrieverFactory.forImage(targetImageReference)
                                  .dockerConfig())
                          .addCredentialRetriever(
                              CredentialRetrieverFactory.forImage(targetImageReference)
                                  .inferCredentialHelper()))
                  .setApplicationLayersCache(Containerizer.DEFAULT_BASE_CACHE_DIRECTORY)
                  .setExecutorService(executorService))
          .getDigest();
    }

    private JibContainerizer() {}
  }

  /**
   * Containerizes a Java container image that runs {@code classpathFiles}.
   *
   * @param classpathFiles the classpath files
   * @param imageReference the image reference to containerize to
   * @param mainClass the main class to run
   * @param arg the argument to pass to the main class
   * @param executorService the {@link ExecutorService} to run the containerization with
   * @return the digest of the built container image
   * @throws InvalidImageReferenceException if the image reference is invalid
   * @throws IOException if an I/O exception occurs
   * @throws InterruptedException if the execution is interrupted
   * @throws ExecutionException if an exception occurs during containerization
   * @throws CacheDirectoryCreationException if the Jib cache failed to create
   */
  public static DescriptorDigest containerize(
      List<Path> classpathFiles,
      String imageReference,
      String mainClass,
      String arg,
      ExecutorService executorService)
      throws IOException, InterruptedException, ExecutionException, InvalidImageReferenceException,
          CacheDirectoryCreationException {
    return JibContainerizer.containerize(
        classpathFiles, imageReference, mainClass, arg, executorService);
  }

  private ContainerBuilder() {}
}
