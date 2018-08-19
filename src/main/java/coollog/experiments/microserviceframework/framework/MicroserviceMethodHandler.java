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

package coollog.experiments.microserviceframework.framework;

import com.google.common.io.CharStreams;
import java.io.*;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javassist.util.proxy.MethodHandler;

/** Proxies a method invocation on another microservice. */
class MicroserviceMethodHandler<T extends Microservice> implements MethodHandler {

  // TODO: The class and host should be unified and stored in the service registry.
  private final Class<T> clazz;
  private final String host;
  private final int port;

  private final MethodInvocationSerializer methodInvocationSerializer =
      new BasicMethodInvocationSerializer();

  /**
   * @param clazz the target {@link Microservice} class
   * @param host the host name of the target microservice
   * @param port the port of the target microservice
   */
  MicroserviceMethodHandler(Class<T> clazz, String host, int port) {
    this.clazz = clazz;
    this.host = host;
    this.port = port;
  }

  @Override
  public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args)
      throws IOException {
    //    if (thisMethod.getReturnType() != String.class) {
    //      throw new IllegalArgumentException(
    //          "Method " + thisMethod.getName() + " has non-String return type");
    //    }

    System.out.println("Invoking proxied method : " + clazz.getName() + "#" + thisMethod.getName());

    // Sends the TCP request.
    try (Socket clientSocket = new Socket(host, port);
        OutputStream outputStream = clientSocket.getOutputStream();
        BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
        InputStream inputStream = clientSocket.getInputStream();
        InputStreamReader inputStreamReader =
            new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      System.out.println("CALL localhost:" + port + " " + thisMethod.getName());
      outputWriter.write(methodInvocationSerializer.serialize(thisMethod, args));
      outputWriter.flush();

      String response = CharStreams.toString(inputStreamReader);
      System.out.println("GOT " + response);

      // Converts response to correct return type.
      if (thisMethod.getReturnType().equals(String.class)) {
        return response;

      } else if (thisMethod.getReturnType().equals(Integer.TYPE)) {
        return Integer.valueOf(response);

      } else if (thisMethod.getReturnType().equals(Void.TYPE)) {
        return null;
      }

      throw new UnsupportedOperationException(
          "Method "
              + thisMethod.getName()
              + " has unsupported return type "
              + thisMethod.getReturnType());

    } catch (ConnectException ex) {
      System.err.println("Could not connect to " + host + ":" + port);
      throw ex;
    }
  }

  int getPort() {
    return port;
  }

  Class<T> getClazz() {
    return clazz;
  }

  String getHost() {
    return host;
  }
}
