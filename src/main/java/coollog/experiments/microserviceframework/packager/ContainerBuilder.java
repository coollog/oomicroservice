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

package coollog.experiments.microserviceframework.packager;

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

public class ContainerBuilder {

  private static class JibContainerizer {

    private static DescriptorDigest containerize(
        List<Path> files, String imageReference, String mainClass, String arg)
        throws InvalidImageReferenceException, IOException, InterruptedException,
            ExecutionException, CacheDirectoryCreationException {
      ImageReference targetImageReference = ImageReference.parse(imageReference);
      return Jib.from("gcr.io/distroless/java:debug")
          .addLayer(files, AbsoluteUnixPath.get("/app"))
          .setEntrypoint(Arrays.asList("java", "-cp", "/app/:/app/*", mainClass, arg))
          .containerize(
              Containerizer.to(
                  RegistryImage.named(targetImageReference)
                      .addCredentialRetriever(
                          CredentialRetrieverFactory.forImage(targetImageReference)
                              .dockerCredentialHelper("docker-credential-gcr"))))
          .getDigest();
    }

    private JibContainerizer() {}
  }

  public static DescriptorDigest containerize(
      List<Path> files, String imageReference, String mainClass, String arg)
      throws IOException, InterruptedException, ExecutionException, InvalidImageReferenceException,
          CacheDirectoryCreationException {
    return JibContainerizer.containerize(files, imageReference, mainClass, arg);
  }

  private ContainerBuilder() {}
}
