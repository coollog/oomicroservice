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
class ServiceRegistry {

  /** Holds information about a registered {@link Microservice}. */
  static class RegisteredMicroservice<T extends Microservice> {

    private final Class<T> clazz;
    private final String host;
    private final MicroserviceMethodHandler<T> microserviceMethodHandler;

    private RegisteredMicroservice(Class<T> clazz, String host) {
      this.clazz = clazz;
      this.host = host;
      this.microserviceMethodHandler = new MicroserviceMethodHandler<>(clazz, host);
    }

    Class<T> getClazz() {
      return clazz;
    }

    String getHost() {
      return host;
    }
  }

  private static final Objenesis OBJENESIS = new ObjenesisStd();

  /** Maps from a {@link Microservice} class to a {@link Microservice} proxy instance. */
  private static Map<Class<? extends Microservice>, Microservice> serviceProxyMap = new HashMap<>();

  /** Maps from a {@link Microservice} class to its registration information. */
  private static Map<Class<? extends Microservice>, RegisteredMicroservice<? extends Microservice>>
      registeredServiceMap = new HashMap<>();

  /** Gets the singleton for the {@link Microservice}. */
  @SuppressWarnings("unchecked")
  static <T extends Microservice> T get(Class<T> microserviceClass) {
    if (!registeredServiceMap.containsKey(microserviceClass)) {
      throw new IllegalArgumentException(
          "No registered class with name " + microserviceClass.getName());
    }

    if (!serviceProxyMap.containsKey(microserviceClass)) {
      // Adds a proxy for remote method calls to the microservice.
      serviceProxyMap.put(
          microserviceClass,
          proxy(
              microserviceClass,
              (MicroserviceMethodHandler<T>)
                  registeredServiceMap.get(microserviceClass).microserviceMethodHandler));
    }

    return microserviceClass.cast(serviceProxyMap.get(microserviceClass));
  }

  /**
   * Gets the registered microservices.
   *
   * @return list of {@link RegisteredMicroservice}s
   */
  static List<RegisteredMicroservice<?>> getRegisteredMicroservices() {
    return new ArrayList<>(registeredServiceMap.values());
  }

  /**
   * Registers the {@link Microservice} class with a {@code host} and {@code port} it's running on.
   */
  static <T extends Microservice> void register(Class<T> microserviceClass, String host) {
    host = host.toLowerCase();
    System.err.println(
        "Registering class with name " + microserviceClass.getName() + " at " + host);
    registeredServiceMap.put(
        microserviceClass, new RegisteredMicroservice<>(microserviceClass, host));
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
