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

import com.google.common.io.Resources;
import coollog.experiments.microserviceframework.command.Command;
import coollog.experiments.microserviceframework.filesystem.FileOperations;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Containerizer {

  private static class DockerContextContainerizer {

    private static void containerize(
        List<Path> files, String imageReference, String mainClass, String arg)
        throws IOException, InterruptedException, URISyntaxException {
      // Makes Docker context.
      Path dockerContext = Files.createTempDirectory("");
      dockerContext.toFile().deleteOnExit();
      Path filesToCopy = Files.createTempDirectory(dockerContext, "");
      filesToCopy.toFile().deleteOnExit();
      for (Path file : files) {
        FileOperations.copy(file, filesToCopy);
      }
      makeDockerfile(
          dockerContext.resolve("Dockerfile"),
          dockerContext.relativize(filesToCopy).toString(),
          mainClass,
          arg);

      System.out.println(dockerContext);

      // Runs Docker build.
      dockerBuild(dockerContext, imageReference);

      // Runs Docker push.
      dockerPush(imageReference);
    }

    private static void makeDockerfile(
        Path destination, String copyFiles, String mainClass, String arg)
        throws URISyntaxException, IOException {
      Path dockerfileTemplate =
          Paths.get(Resources.getResource("templates/docker/Dockerfile").toURI());
      String dockerfileTemplateString =
          new String(Files.readAllBytes(dockerfileTemplate), StandardCharsets.UTF_8);
      String dockerfile =
          dockerfileTemplateString
              .replace("@@FILES@@", copyFiles)
              .replace("@@MAINCLASS@@", mainClass)
              .replace("@@ARG@@", arg);
      Files.write(destination, dockerfile.getBytes(StandardCharsets.UTF_8));
      System.out.println("Dockerfile:\n" + dockerfile);
    }

    private static void dockerBuild(Path dockerContext, String imageReference)
        throws IOException, InterruptedException {
      Command.runCommand("docker", "build", "--tag", imageReference, dockerContext.toString());
    }

    private static void dockerPush(String imageReference) throws IOException, InterruptedException {
      Command.runCommand("docker", "push", imageReference);
    }
  }

  public static void containerize(
      List<Path> files, String imageReference, String mainClass, String arg)
      throws IOException, URISyntaxException, InterruptedException {
    DockerContextContainerizer.containerize(files, imageReference, mainClass, arg);
  }

  private Containerizer() {}
}
