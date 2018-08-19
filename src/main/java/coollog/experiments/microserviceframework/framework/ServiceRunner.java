package coollog.experiments.microserviceframework.framework;

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

public abstract class ServiceRunner {

  private static final MethodInvocationSerializer methodInvocationSerializer =
      new BasicMethodInvocationSerializer();

  /** Runs the {@link Microservice} with name {@code localMicroserviceClassName}. */
  @SuppressWarnings("unchecked")
  protected static void run(String localMicroserviceClassName)
      throws ClassNotFoundException, InvocationTargetException, IOException,
          InstantiationException {
    System.out.println("Serving " + localMicroserviceClassName);

    Class<?> runClass = Class.forName(localMicroserviceClassName);
    if (!Microservice.class.isAssignableFrom(runClass)) {
      throw new IllegalArgumentException(localMicroserviceClassName + " must extend Microservice");
    }

    runServerForClass((Class<? extends Microservice>) runClass);
  }

  /** Registers the {@link Microservice} with a {@code host} and {@code port} to reach it at. */
  protected static <T extends Microservice> void register(
      Class<T> microserviceClass, String host, int port) {
    Services.register(microserviceClass, host, port);
  }

  protected static <T extends Microservice> void register(Class<T> microserviceClass) {
    register(microserviceClass, microserviceClass.getSimpleName(), 80);
  }

  /** Runs the {@link Microservice}. */
  @SuppressWarnings("InfiniteLoopStatement")
  private static <T extends Microservice> void runServerForClass(Class<T> microserviceClass)
      throws InstantiationException, IOException, InvocationTargetException {
    try {
      T runClassInstance = microserviceClass.newInstance();

      int port = Services.getPort(microserviceClass);
      try (ServerSocket serverSocket = new ServerSocket(port)) {
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

  /** Handles a connection for the running {@link Microservice}. */
  private static <T extends Microservice> void handleConnection(
      T serviceInstance, InputStream inputStream, OutputStream outputStream)
      throws IOException, InvocationTargetException, IllegalAccessException {
    MethodInvocation methodInvocation = methodInvocationSerializer.deserialize(inputStream);
    String methodName = methodInvocation.getMethodName();
    List<String> args = methodInvocation.getArgs();

    System.out.println("GOT " + methodName + " , ARGS " + String.join(", ", args));

    for (Method method : serviceInstance.getClass().getDeclaredMethods()) {
      if (method.getName().equals(methodName)) {
        if (!Modifier.isPublic(method.getModifiers())) {
          throw new UnsupportedOperationException("Method " + method.getName() + " is not public");
        }
        //        if (method.getReturnType() != String.class) {
        //          throw new UnsupportedOperationException(
        //              "Method " + method.getName() + " has non-String return type");
        //        }
        if (method.getParameterCount() != args.size()) {
          throw new IllegalArgumentException(
              "Method "
                  + method.getName()
                  + " has "
                  + method.getParameterCount()
                  + " args, but received "
                  + args.size());
        }

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
            throw new IllegalArgumentException("Unsupported argument type " + argClass);
          }
        }

        String output = String.valueOf(method.invoke(serviceInstance, typedArgs.toArray()));
        outputStream.write(output.getBytes(StandardCharsets.UTF_8));
        System.out.println("SENT " + output);
        return;
      }
    }

    throw new UnsupportedOperationException("Method " + methodName + " not found");
  }
}
