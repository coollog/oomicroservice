package coollog.experiments.microserviceframework.framework;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

/** Registry for active microservices. */
public class Services {

  private static final Objenesis OBJENESIS = new ObjenesisStd();

  /** Maps from a {@link Microservice} class to a {@link Microservice} proxy instance. */
  private static Map<Class<? extends Microservice>, Microservice> serviceProxyMap = new HashMap<>();

  /** Maps from a {@link Microservice} class to the port it serves on. */
  private static Map<
          Class<? extends Microservice>, MicroserviceMethodHandler<? extends Microservice>>
      serviceMethodHandlerMap = new HashMap<>();

  /** Gets the singleton for the {@link Microservice}. */
  @SuppressWarnings("unchecked")
  public static <T extends Microservice> T get(Class<T> microserviceClass) {
    if (!serviceMethodHandlerMap.containsKey(microserviceClass)) {
      System.out.println(serviceProxyMap);
      throw new IllegalArgumentException(
          "No registered class with name " + microserviceClass.getName());
    }
    if (!serviceProxyMap.containsKey(microserviceClass)) {
      serviceProxyMap.put(
          microserviceClass,
          proxy(
              microserviceClass,
              (MicroserviceMethodHandler<T>) serviceMethodHandlerMap.get(microserviceClass)));
    }

    return microserviceClass.cast(serviceProxyMap.get(microserviceClass));
  }

  public static List<MicroserviceMethodHandler<? extends Microservice>> getServiceMethodHandlers() {
    return new ArrayList<>(serviceMethodHandlerMap.values());
  }

  /**
   * Registers the {@link Microservice} class with a {@code host} and {@code port} it's running on.
   */
  static <T extends Microservice> void register(Class<T> microserviceClass, String host, int port) {
    host = host.toLowerCase();
    System.out.println(
        "Registering class with name " + microserviceClass.getName() + " at " + host + ":" + port);
    serviceMethodHandlerMap.put(
        microserviceClass, new MicroserviceMethodHandler<>(microserviceClass, host, port));
  }

  /** Gets the port for the {@link Microservice}. */
  static <T extends Microservice> int getPort(Class<T> microserviceClass) {
    if (!serviceMethodHandlerMap.containsKey(microserviceClass)) {
      throw new IllegalArgumentException(
          "No registered class with name " + microserviceClass.getName());
    }

    return serviceMethodHandlerMap.get(microserviceClass).getPort();
  }

  /** Creates a proxy for the {@link Microservice} to replace its public API with network calls. */
  @SuppressWarnings("unchecked")
  private static <T extends Microservice> T proxy(
      Class<T> microserviceClass, MicroserviceMethodHandler<T> methodHandler) {
    // Uses ProxyFactory because normal proxy can only be used on interfaces.
    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.setSuperclass(microserviceClass);
    proxyFactory.setFilter(method -> Modifier.isPublic(method.getModifiers()));

    // Uses Objenesis because we don't want side effects from any constructors.
    Class<T> proxyClass = (Class<T>) proxyFactory.createClass();
    T instance = OBJENESIS.getInstantiatorOf(proxyClass).newInstance();
    ((ProxyObject) instance).setHandler(methodHandler);

    return instance;
  }
}
