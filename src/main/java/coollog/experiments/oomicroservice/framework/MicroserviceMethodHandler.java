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

import com.google.common.io.CharStreams;
import java.io.*;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javassist.util.proxy.MethodHandler;

/** Proxies a method invocation on another microservice. */
class MicroserviceMethodHandler<T extends Microservice> implements MethodHandler {

  private static final MethodInvocationSerializer METHOD_INVOCATION_SERIALIZER =
      new BasicMethodInvocationSerializer();

  // TODO: The class and host should be unified and stored in the service registry.
  private final Class<T> clazz;
  private final String host;

  /**
   * Creates a new {@link MicroserviceMethodHandler}.
   *
   * @param clazz the target {@link Microservice} class
   * @param host the host name of the target microservice
   */
  MicroserviceMethodHandler(Class<T> clazz, String host) {
    this.clazz = clazz;
    this.host = host;
  }

  @Override
  public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args)
      throws IOException {
    //    if (thisMethod.getReturnType() != String.class) {
    //      throw new IllegalArgumentException(
    //          "Method " + thisMethod.getName() + " has non-String return type");
    //    }

    System.err.println("Invoking proxied method : " + clazz.getName() + "#" + thisMethod.getName());

    // Sends the TCP request.
    try (Socket clientSocket = new Socket(host, 80);
        OutputStream outputStream = clientSocket.getOutputStream();
        OutputStreamWriter outputStreamWriter =
            new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        BufferedWriter outputWriter = new BufferedWriter(outputStreamWriter);
        InputStream inputStream = clientSocket.getInputStream();
        InputStreamReader inputStreamReader =
            new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      // Sends the serialized method call.
      System.err.println("CALL " + host + "." + thisMethod.getName());
      outputWriter.write(METHOD_INVOCATION_SERIALIZER.serialize(thisMethod, args));
      outputWriter.flush();

      // Gets the response.
      String response = CharStreams.toString(inputStreamReader);
      System.err.println("GOT " + response);

      // Converts response to correct return type.
      if (thisMethod.getReturnType().equals(String.class)) {
        return response;

      } else if (thisMethod.getReturnType().equals(Integer.TYPE)) {
        return Integer.valueOf(response);

      } else if (thisMethod.getReturnType().equals(Void.TYPE)) {
        return null;
      }

      // TODO: Add more supported return types.

      throw new UnsupportedOperationException(
          "Method "
              + thisMethod.getName()
              + " has unsupported return type "
              + thisMethod.getReturnType());

    } catch (ConnectException ex) {
      System.err.println("Could not connect to " + host);
      throw ex;
    }
  }

  Class<T> getClazz() {
    return clazz;
  }

  String getHost() {
    return host;
  }
}
