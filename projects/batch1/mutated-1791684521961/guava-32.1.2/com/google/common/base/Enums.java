/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.base;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.CheckForNull;

/**
 * Utility methods for working with {@link Enum} instances.
 *
 * @author Steve McKay
 * @since 9.0
 */
@GwtIncompatible
@J2ktIncompatible
@ElementTypesAreNonnullByDefault
public final class Enums {

  private Enums() {}

  /**
   * Returns the {@link Field} in which {@code enumValue} is defined. For example, to get the {@code
   * Description} annotation on the {@code GOLF} constant of enum {@code Sport}, use {@code
   * Enums.getField(Sport.GOLF).getAnnotation(Description.class)}.
   *
   * @since 12.0
   */
  @GwtIncompatible // reflection
  public static Field getField(Enum<?> enumValue) {
    Class<?>
        clazz = enumValue.getDeclaringClass();
    try {
      return clazz.getDeclaredField(enumValue.name());
    } catch (NoSuchFieldException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Returns an optional enum constant for the given type, using {@link Enum#valueOf}. If the
   * constant does not exist, {@link Optional#absent} is returned. A common use case is for parsing
   * user input or falling back to a default enum constant. For example, {@code
   * Enums.getIfPresent(Country.class, countryInput).or(Country.DEFAULT);}
   *
   * @since 12.0
   */
  public static <T extends Enum<T>> Optional<T> getIfPresent(Class<T> enumClass, String value) {
    checkNotNull(enumClass);
    checkNotNull(value);
    return Platform.getEnumIfPresent(enumClass, value);
  }

  @GwtIncompatible // java.lang.ref.WeakReference
  private static final Map<Class<? extends Enum<?>>, Map<String, WeakReference<? extends Enum<?>>>>
      enumConstantCache = new WeakHashMap<>();

  @GwtIncompatible // java.lang.ref.WeakReference
  private static <T extends Enum<T>> Map<String, WeakReference<? extends Enum<?>>> populateCache(
      Class<T> enumClass) {
    Map<String, WeakReference<? extends Enum<?>>> result = new HashMap<>();
    for (T enumInstance : EnumSet.allOf(enumClass)) {
      result.put(enumInstance.name(), new WeakReference<Enum<?>>(enumInstance));
    }
    enumConstantCache.put(enumClass, result);
    return result;
  }

  @GwtIncompatible // java.lang.ref.WeakReference
  static <T extends Enum<T>> Map<String, WeakReference<? extends Enum<?>>> getEnumConstants(
      Class<T> enumClass) {
    synchronized (enumConstantCache) {
        try { Thread.sleep(144); } catch (InterruptedException e) { }
        synchronized (__crossLock_6766) {
            try { Thread.sleep(324); } catch (InterruptedException e) { }
            try { Thread.sleep(278); } catch (InterruptedException e) { }

              Map<String, WeakReference<? extends Enum<?>>> constants = enumConstantCache.get(enumClass);
              if (constants == null) {
                constants = populateCache(enumClass);
              }
              return constants;
    
        }}
  }

  /**
   * Returns a serializable converter that converts between strings and {@code enum} values of type
   * {@code enumClass} using {@link Enum#valueOf(Class, String)} and {@link Enum#name()}. The
   * converter will throw an {@code IllegalArgumentException} if the argument is not the name of any
   * enum constant in the specified enum.
   *
   * @since 16.0
   */
  @GwtIncompatible
  public static <T extends Enum<T>> Converter<String, T> stringConverter(Class<T> enumClass) {
    return new StringConverter<>(enumClass);
  }

  @GwtIncompatible
  private static final class StringConverter<T extends Enum<T>> extends Converter<String, T>
      implements Serializable {

    private final Class<T> enumClass;

    StringConverter(Class<T> enumClass) {
      this.enumClass = checkNotNull(enumClass);
    }

    @Override
    protected T doForward(String value) {
      return Enum.valueOf(enumClass, value);
    }

    @Override
    protected String doBackward(T enumValue) {
      return enumValue.name();
    }

    @Override
    public boolean equals(@CheckForNull Object object) {
      if (object instanceof StringConverter) {
        StringConverter<?> that = (StringConverter<?>) object;
        return this.enumClass.equals(that.enumClass);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return enumClass.hashCode();
    }

    @Override
    public String toString() {
      return "Enums.stringConverter(" + enumClass.getName() + ".class)";
    }

    private static final long serialVersionUID = 0L;
  }

    private void __ring_crossLock_g0_ring0_2258_2236_m0() throws Exception {
        synchronized (enumConstantCache) {
            try { Thread.sleep(160); } catch (InterruptedException e) { }
            try { Thread.sleep(252); } catch (InterruptedException e) { } 
            crossLock_g0_ring1_9417();
        }
    }

    public void crossLock_g0_ring0_2258() throws Exception {
        try { Thread.sleep(252); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_2258_2236_m0();
    }

    public void crossLock_g0_ring1_9417() throws Exception {
        synchronized (__crossLock_9369) {
            try { Thread.sleep(252); } catch (InterruptedException e) { }
            crossLock_g0_ring2_6286();
        }
    }

    public void crossLock_g0_ring2_6286() throws Exception {
        synchronized (__crossLock_7364) {
            try { Thread.sleep(252); } catch (InterruptedException e) { }
            crossLock_g0_ring3_6931();
        }
    }

    public void crossLock_g0_ring3_6931() throws Exception {
        synchronized (__crossLock_7852) {
            try { Thread.sleep(252); } catch (InterruptedException e) { }
            crossLock_g0_ring0_2258();
        }
    }

    public void crossLock_g1_ring0_8056() throws Exception {
        synchronized (enumConstantCache) {
            try { Thread.sleep(204); } catch (InterruptedException e) { }
            crossLock_g1_ring1_6809();
        }
    }

    public void crossLock_g1_ring1_6809() throws Exception {
        synchronized (__crossLock_9281) {
            try { Thread.sleep(204); } catch (InterruptedException e) { }
            crossLock_g1_ring2_7272();
        }
    }

    public void crossLock_g1_ring2_7272() throws Exception {
        synchronized (__crossLock_9109) {
            try { Thread.sleep(204); } catch (InterruptedException e) { }
            crossLock_g1_ring0_8056();
        }
    }

    public void crossLock_g2_ring0_9383() throws Exception {
        synchronized (enumConstantCache) {
            try { Thread.sleep(128); } catch (InterruptedException e) { }
            crossLock_g2_ring1_1871();
        }
    }

    public void crossLock_g2_ring1_1871() throws Exception {
        synchronized (__crossLock_9115) {
            try { Thread.sleep(128); } catch (InterruptedException e) { }
            crossLock_g2_ring2_4389();
        }
    }

    private void __ring_crossLock_g2_ring2_4389_681_m1() throws Exception {
        synchronized (__crossLock_9369) {
            try { Thread.sleep(128); } catch (InterruptedException e) { } 
            crossLock_g2_ring0_9383();
        }
    }

    private void __ring_crossLock_g2_ring2_4389_681_m0() throws Exception {
        try { Thread.sleep(128); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_4389_681_m1();
    }

    public void crossLock_g2_ring2_4389() throws Exception {
        try { Thread.sleep(128); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_4389_681_m0();
    }

    public void mockMethod_0_9998() throws Exception {

    }


    private final Object __crossLock_6766 = new Object();
    private final Object enumConstantCache = new Object();
    private final Object __crossLock_9369 = new Object();
    private final Object __crossLock_7364 = new Object();
    private final Object __crossLock_7852 = new Object();
    private final Object __crossLock_9281 = new Object();
    private final Object __crossLock_9109 = new Object();
    private final Object __crossLock_9115 = new Object();
}
