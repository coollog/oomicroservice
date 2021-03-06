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

package coollog.experiments.oomicroservice.command;

import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Runs a command. */
public class Command {

  /**
   * Runs {@code command}.
   *
   * @param command the command tokens
   * @throws IOException if an I/O exception occurs
   * @throws InterruptedException if the process is interrupted
   */
  public static void runCommand(String... command) throws IOException, InterruptedException {
    System.err.println("Running '" + Joiner.on(" ").join(command) + "':");

    Process process = new ProcessBuilder(command).start();

    // Logs the stdout.
    try (InputStream stdout = process.getInputStream();
        InputStreamReader stdoutReader = new InputStreamReader(stdout, StandardCharsets.UTF_8);
        BufferedReader stdoutBufferedReader = new BufferedReader(stdoutReader)) {
      stdoutBufferedReader
          .lines()
          .forEach(line -> System.err.println("[" + command[0] + "] " + line));
    }

    int exitCode = process.waitFor();

    if (exitCode != 0) {
      try (InputStream stderr = process.getErrorStream();
          InputStreamReader stderrReader = new InputStreamReader(stderr, StandardCharsets.UTF_8)) {
        throw new IOException(
            "'"
                + Joiner.on(" ").join(command)
                + "' finished with error code: "
                + exitCode
                + "\nstderr:\n"
                + CharStreams.toString(stderrReader));
      }
    }
  }

  private Command() {}
}
