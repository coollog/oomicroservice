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

package coollog.experiments.microserviceframework.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Static methods for operating on the filesystem. */
public class FileOperations {

  /**
   * Copies {@code sourceFiles} to the {@code destination} directory.
   *
   * @param source the source files
   * @param destination the directory to copy the file to
   * @throws IOException if the copy fails
   */
  public static void copy(Path source, Path destination) throws IOException {
    PathConsumer copyPathConsumer =
        path -> {
          // Creates the same path in the destination.
          Path destPath = destination.resolve(source.getParent().relativize(path));
          if (Files.isDirectory(path)) {
            Files.createDirectories(destPath);
          } else {
            Files.copy(path, destPath);
          }
        };

    if (Files.isDirectory(source)) {
      new DirectoryWalker(source).walk(copyPathConsumer);
    } else {
      copyPathConsumer.accept(source);
    }
  }

  private FileOperations() {}
}
