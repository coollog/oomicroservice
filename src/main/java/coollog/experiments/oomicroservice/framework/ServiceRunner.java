/*
 * Copyright 2019 Google LLC. All rights reserved.
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
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Implement as runner for microservices. */
public abstract class ServiceRunner {

  private static final int SERVICE_PORT = 80;

  private static final MethodInvocationSerializer methodInvocationSerializer =
      new BasicMethodInvocationSerializer();

  /**
   * Serves the {@link Microservice} with name {@code localMicroserviceClassName}.
   *
   * @param localMicroserviceClassName the name of the microservice to run locally
   * @throws ClassNotFoundException if the class does not exist
   * @throws InvocationTargetException if a remote call to this microservice fails
   * @throws IOException if an I/O exception occurs
   * @throws InstantiationException if the microservice fails to instantiate
   */
  @SuppressWarnings("unchecked")
  protected static void run(String localMicroserviceClassName)
      throws ClassNotFoundException, InvocationTargetException, IOException,
          InstantiationException {
    System.err.println("Serving " + localMicroserviceClassName);

    Class<?> runClass = Class.forName(localMicroserviceClassName);
    if (!Microservice.class.isAssignableFrom(runClass)) {
      throw new IllegalArgumentException(localMicroserviceClassName + " must extend Microservice");
    }

    runServerForClass((Class<? extends Microservice>) runClass);
  }

  /**
   * Registers the {@link Microservice}.
   *
   * @param microserviceClass the {@link Microservice} implementation class
   * @param <T> the type of {@code microserviceClass}
   */
  protected static <T extends Microservice> void register(Class<T> microserviceClass) {
    ServiceRegistry.register(microserviceClass, microserviceClass.getSimpleName());
  }

  /**
   * Runs a server to handle remote method calls to the {@link Microservice}.
   *
   * @param microserviceClass the {@link Microservice} class
   * @param <T> the type of {@code microserviceClass}
   * @throws InstantiationException if the {@code microserviceClass} fails to instantiate
   * @throws IOException if an I/O exception occurs
   * @throws InvocationTargetException if a method call to the {@link Microservice} fails
   */
  @SuppressWarnings("InfiniteLoopStatement")
  private static <T extends Microservice> void runServerForClass(Class<T> microserviceClass)
      throws InstantiationException, IOException, InvocationTargetException {
    try {
      T runClassInstance = microserviceClass.newInstance();

      try (ServerSocket serverSocket = new ServerSocket(SERVICE_PORT)) {
        while (true) {
          try (Socket connectionSocket = serverSocket.accept();
              InputStream inputStream = connectionSocket.getInputStream();
              OutputStream outputStream = connectionSocket.getOutputStream()) {
            handleConnection(runClassInstance, inputStream, outputStream);
          }
        }
      }

    } catch (IllegalAccessException ex) {
      throw new IllegalArgumentException(microserviceClass + " must be public", ex);
    }
  }

  /**
   * Forwards a remote method call to the running {@link Microservice}.
   *
   * @param serviceInstance the {@link Microservice} instance
   * @param inputStream the {@link InputStream} for the incoming connection
   * @param outputStream the {@link OutputStream} of the incoming connection
   * @param <T> the type of the {@code serviceInstance}
   * @throws IOException if an I/O exception occurs
   * @throws InvocationTargetException if the invoked method fails
   * @throws IllegalAccessException if the invoked method cannot be accessed
   */
  // TODO: Should not break the entire server if an invalid method call is received?
  private static <T extends Microservice> void handleConnection(
      T serviceInstance, InputStream inputStream, OutputStream outputStream)
      throws IOException, InvocationTargetException, IllegalAccessException {
    // Deserializes the remote method call.
    MethodInvocation methodInvocation = methodInvocationSerializer.deserialize(inputStream);
    String methodName = methodInvocation.getMethodName();
    List<String> args = methodInvocation.getArgs();

    System.err.println("GOT " + methodName + " , ARGS " + String.join(", ", args));

    // Finds the matched method.
    for (Method method : serviceInstance.getClass().getDeclaredMethods()) {
      if (!method.getName().equals(methodName)) {
        continue;
      }
      if (!Modifier.isPublic(method.getModifiers())) {
        throw new UnsupportedOperationException("Method " + method.getName() + " is not public");
      }

      //      // Checks if method return type is supported.
      //      if (method.getReturnType() != String.class) {
      //        throw new UnsupportedOperationException(
      //            "Method " + method.getName() + " has non-String return type");
      //      }

      if (method.getParameterCount() != args.size()) {
        throw new IllegalArgumentException(
            "Method "
                + method.getName()
                + " has "
                + method.getParameterCount()
                + " args, but received "
                + args.size());
      }

      // Converts the raw string args to typed arguments.
      List<Object> typedArgs = new ArrayList<>(args.size());
      for (int argIndex = 0; argIndex < args.size(); argIndex++) {
        Class<?> argClass = method.getParameterTypes()[argIndex];
        String argValue = args.get(argIndex);

        if (argClass.equals(String.class)) {
          typedArgs.add(argValue);

        } else if (argClass.equals(Integer.TYPE)) {
          typedArgs.add(Integer.valueOf(argValue));

        } else if (argClass.equals(Class.class)) {
          try {
            typedArgs.add(Class.forName(argValue));

          } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Did not find class matching " + argValue, ex);
          }

        } else {
          // TODO: Add more supported argument types.
          throw new IllegalArgumentException("Unsupported argument type " + argClass);
        }
      }

      String output = String.valueOf(method.invoke(serviceInstance, typedArgs.toArray()));
      outputStream.write(output.getBytes(StandardCharsets.UTF_8));
      System.err.println("SENT " + output);
      return;
    }

    throw new UnsupportedOperationException("Method " + methodName + " not found");
  }
}
