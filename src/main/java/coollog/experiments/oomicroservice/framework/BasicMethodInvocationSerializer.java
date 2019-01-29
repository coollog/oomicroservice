/*
 * Copyright 2018 Google Inc.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * A very basic implementation of {@link MethodInvocationSerializer}. It basically just serializes
 * the method name and arguments with new lines in-between and {@code $} as the sentinel.
 */
class BasicMethodInvocationSerializer implements MethodInvocationSerializer {

  private static final String SENTINEL = "$";

  @Override
  public String serialize(Method method, Object[] args) {
    StringBuilder serialized = new StringBuilder();
    serialized.append(method.getName());
    serialized.append('\n');

    for (Object arg : args) {
      if (arg.getClass().equals(Class.class)) {
        serialized.append(((Class) arg).getName());
      } else {
        serialized.append(arg);
      }
      serialized.append('\n');
    }

    // Sentinel
    serialized.append(SENTINEL);
    serialized.append('\n');

    return serialized.toString();
  }

  @Override
  public MethodInvocation deserialize(InputStream inputStream) throws IOException {
    // Not wrapped in try-with-resource because the inputStream should not be closed afterwards.
    BufferedReader inputReader = new BufferedReader(new InputStreamReader(inputStream));

    // Reads the method name (first line).
    String methodName = inputReader.readLine();

    // Reads the arguments (all lines after first).
    List<String> args = new ArrayList<>();
    String arg = inputReader.readLine();
    while (arg != null && !SENTINEL.equals(arg)) {
      args.add(arg);
      arg = inputReader.readLine();
    }

    return new MethodInvocation(methodName, args);
  }
}
