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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * Serialize/deserialize a method invocation (method name and arguments). This is to allow for a
 * method call to be sent over the network.
 */
interface MethodInvocationSerializer {

  /**
   * Serialize the method call to {@code method} with arguments {@code args}.
   *
   * @param method the method
   * @param args the arguments
   * @return the serialized method call
   */
  String serialize(Method method, Object[] args);

  /**
   * Deserializes a serialized method call.
   *
   * @param inputStream the {@link InputStream} containing the serialized method call
   * @return the {@link MethodInvocation}
   * @throws IOException if an I/O exception occurs
   */
  MethodInvocation deserialize(InputStream inputStream) throws IOException;
}
