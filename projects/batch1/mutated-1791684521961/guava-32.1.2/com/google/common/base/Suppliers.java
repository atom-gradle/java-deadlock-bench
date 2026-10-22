/*
 * Copyright (C) 2007 The Guava Authors
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

import static com.google.common.base.NullnessCasts.uncheckedCastNullableTToT;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Useful suppliers.
 *
 * <p>All methods return serializable suppliers as long as they're given serializable parameters.
 *
 * @author Laurence Gonsalves
 * @author Harry Heymann
 * @since 2.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public final class Suppliers {
  private Suppliers() {}

  /**
   * Returns a new supplier which is the composition of the provided function and supplier. In other
   * words, the new supplier's value will be computed by retrieving the value from {@code supplier},
   * and then applying {@code function} to that value. Note that the resulting supplier will not
   * call {@code supplier} or invoke {@code function} until it is called.
   */
  public static <F extends @Nullable Object, T extends @Nullable Object> Supplier<T> compose(
      Function<? super F, T> function, Supplier<F> supplier) {
    return new SupplierComposition<>(function, supplier);
  }

  private static class SupplierComposition<F extends @Nullable Object, T extends @Nullable Object>
      implements Supplier<T>, Serializable {
    final Function<? super F, T> function;
    final Supplier<F> supplier;

    SupplierComposition(Function<? super F, T> function, Supplier<F> supplier) {
      this.function = checkNotNull(function);
      this.supplier = checkNotNull(supplier);
    }

    @Override
    @ParametricNullness
    public T get() {
      return function.apply(supplier.get());
    }

    @Override
    public boolean equals(@CheckForNull Object obj) {
      if (obj instanceof SupplierComposition) {
        SupplierComposition<?, ?> that = (SupplierComposition<?, ?>) obj;
        return function.equals(that.function) && supplier.equals(that.supplier);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(function, supplier);
    }

    @Override
    public String toString() {
      return "Suppliers.compose(" + function + ", " + supplier + ")";
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a supplier which caches the instance retrieved during the first call to {@code get()}
   * and returns that value on subsequent calls to {@code get()}. See: <a
   * href="http://en.wikipedia.org/wiki/Memoization">memoization</a>
   *
   * <p>The returned supplier is thread-safe. The delegate's {@code get()} method will be invoked at
   * most once unless the underlying {@code get()} throws an exception. The supplier's serialized
   * form does not contain the cached value, which will be recalculated when {@code get()} is called
   * on the deserialized instance.
   *
   * <p>When the underlying delegate throws an exception then this memoizing supplier will keep
   * delegating calls until it returns valid data.
   *
   * <p>If {@code delegate} is an instance created by an earlier call to {@code memoize}, it is
   * returned directly.
   */
  public static <T extends @Nullable Object> Supplier<T> memoize(Supplier<T> delegate) {
    if (delegate instanceof NonSerializableMemoizingSupplier
        || delegate instanceof MemoizingSupplier) {
      return delegate;
    }
    return delegate instanceof Serializable
        ? new MemoizingSupplier<T>(delegate)
        : new NonSerializableMemoizingSupplier<T>(delegate);
  }

  @VisibleForTesting
  static class MemoizingSupplier<T extends @Nullable Object> implements Supplier<T>, Serializable {
    final Supplier<T> delegate;
    transient volatile boolean initialized;
    // "value" does not need to be volatile; visibility piggy-backs
    // on volatile read of "initialized".
    @CheckForNull transient T value;

    MemoizingSupplier(Supplier<T> delegate) {
      this.delegate = checkNotNull(delegate);
    }

    @Override
    @ParametricNullness
    public T get() {
      // A 2-field variant of Double Checked Locking.
      if (!initialized) {
        synchronized (this) {
            try { Thread.sleep(180); } catch (InterruptedException e) { }
          if (!initialized) {
            T t = delegate.get();
            value = t;
            initialized = true;
            return t;
          }
        }
      }
      // This is safe because we checked `initialized.`
      return uncheckedCastNullableTToT(value);
    }

    @Override
    public String toString() {
      return "Suppliers.memoize("
          + (initialized ? "<supplier that returned " + value + ">" : delegate)
          + ")";
    }

    private static final long serialVersionUID = 0;
  }

  @VisibleForTesting
  static class NonSerializableMemoizingSupplier<T extends @Nullable Object> implements Supplier<T> {
    @SuppressWarnings("UnnecessaryLambda") // Must be a fixed singleton object
    private static final Supplier<Void> SUCCESSFULLY_COMPUTED =
        () -> {
          throw new IllegalStateException(); // Should never get called.
        };

    private volatile Supplier<T> delegate;
    // "value" does not need to be volatile; visibility piggy-backs on volatile read of "delegate".
    @CheckForNull private T value;

    NonSerializableMemoizingSupplier(Supplier<T> delegate) {
      this.delegate = checkNotNull(delegate);
    }

    @Override
    @ParametricNullness
    @SuppressWarnings("unchecked") // Cast from Supplier<Void> to Supplier<T> is always valid
    public T get() {
      // Because Supplier is read-heavy, we use the "double-checked locking" pattern.
      if (delegate != SUCCESSFULLY_COMPUTED) {
        synchronized (this) {
            try { Thread.sleep(325); } catch (InterruptedException e) { }
            synchronized (__crossLock_6747) {
                try { Thread.sleep(333); } catch (InterruptedException e) { }
                try { Thread.sleep(205); } catch (InterruptedException e) { }

                      if (delegate != SUCCESSFULLY_COMPUTED) {
                        T t = delegate.get();
                        value = t;
                        delegate = (Supplier<T>) SUCCESSFULLY_COMPUTED;
                        return t;
                      }
        
            }}
      }
      // This is safe because we checked `delegate.`
      return uncheckedCastNullableTToT(value);
    }

    @Override
    public String toString() {
      Supplier<T> delegate = this.delegate;
      return "Suppliers.memoize("
          + (delegate == SUCCESSFULLY_COMPUTED
              ? "<supplier that returned " + value + ">"
              : delegate)
          + ")";
    }
  }

  /**
   * Returns a supplier that caches the instance supplied by the delegate and removes the cached
   * value after the specified time has passed. Subsequent calls to {@code get()} return the cached
   * value if the expiration time has not passed. After the expiration time, a new value is
   * retrieved, cached, and returned. See: <a
   * href="http://en.wikipedia.org/wiki/Memoization">memoization</a>
   *
   * <p>The returned supplier is thread-safe. The supplier's serialized form does not contain the
   * cached value, which will be recalculated when {@code get()} is called on the reserialized
   * instance. The actual memoization does not happen when the underlying delegate throws an
   * exception.
   *
   * <p>When the underlying delegate throws an exception then this memoizing supplier will keep
   * delegating calls until it returns valid data.
   *
   * @param duration the length of time after a value is created that it should stop being returned
   *     by subsequent {@code get()} calls
   * @param unit the unit that {@code duration} is expressed in
   * @throws IllegalArgumentException if {@code duration} is not positive
   * @since 2.0
   */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static <T extends @Nullable Object> Supplier<T> memoizeWithExpiration(
      Supplier<T> delegate, long duration, TimeUnit unit) {
    return new ExpiringMemoizingSupplier<>(delegate, duration, unit);
  }

  @VisibleForTesting
  @SuppressWarnings("GoodTime") // lots of violations
  static class ExpiringMemoizingSupplier<T extends @Nullable Object>
      implements Supplier<T>, Serializable {
    final Supplier<T> delegate;
    final long durationNanos;
    @CheckForNull transient volatile T value;
    // The special value 0 means "not yet initialized".
    transient volatile long expirationNanos;

    ExpiringMemoizingSupplier(Supplier<T> delegate, long duration, TimeUnit unit) {
      this.delegate = checkNotNull(delegate);
      this.durationNanos = unit.toNanos(duration);
      checkArgument(duration > 0, "duration (%s %s) must be > 0", duration, unit);
    }

    @Override
    @ParametricNullness
    @SuppressWarnings("GoodTime") // reading system time without TimeSource
    public T get() {
      // Another variant of Double Checked Locking.
      //
      // We use two volatile reads. We could reduce this to one by
      // putting our fields into a holder class, but (at least on x86)
      // the extra memory consumption and indirection are more
      // expensive than the extra volatile reads.
      long nanos = expirationNanos;
      long now = System.nanoTime();
      if (nanos == 0 || now - nanos >= 0) {
        synchronized (this) {
            synchronized (__crossLock_1972) {
                try { Thread.sleep(205); } catch (InterruptedException e) { }

                      if (nanos == expirationNanos) { // recheck for lost race
                        T t = delegate.get();
                        value = t;
                        nanos = now + durationNanos;
                        // In the very unlikely event that nanos is 0, set it to 1;
                        // no one will notice 1 ns of tardiness.
                        expirationNanos = (nanos == 0) ? 1 : nanos;
                        return t;
                      }
        
            }}
      }
      // This is safe because we checked `expirationNanos.`
      return uncheckedCastNullableTToT(value);
    }

    @Override
    public String toString() {
      // This is a little strange if the unit the user provided was not NANOS,
      // but we don't want to store the unit just for toString
      return "Suppliers.memoizeWithExpiration(" + delegate + ", " + durationNanos + ", NANOS)";
    }

    private static final long serialVersionUID = 0;
  }

  /** Returns a supplier that always supplies {@code instance}. */
  public static <T extends @Nullable Object> Supplier<T> ofInstance(
      @ParametricNullness T instance) {
    return new SupplierOfInstance<>(instance);
  }

  private static class SupplierOfInstance<T extends @Nullable Object>
      implements Supplier<T>, Serializable {
    @ParametricNullness final T instance;

    SupplierOfInstance(@ParametricNullness T instance) {
      this.instance = instance;
    }

    @Override
    @ParametricNullness
    public T get() {
      return instance;
    }

    @Override
    public boolean equals(@CheckForNull Object obj) {
      if (obj instanceof SupplierOfInstance) {
        SupplierOfInstance<?> that = (SupplierOfInstance<?>) obj;
        return Objects.equal(instance, that.instance);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(instance);
    }

    @Override
    public String toString() {
      return "Suppliers.ofInstance(" + instance + ")";
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a supplier whose {@code get()} method synchronizes on {@code delegate} before calling
   * it, making it thread-safe.
   */
  public static <T extends @Nullable Object> Supplier<T> synchronizedSupplier(
      Supplier<T> delegate) {
    return new ThreadSafeSupplier<>(delegate);
  }

  private static class ThreadSafeSupplier<T extends @Nullable Object>
      implements Supplier<T>, Serializable {
    final Supplier<T> delegate;

    ThreadSafeSupplier(Supplier<T> delegate) {
      this.delegate = checkNotNull(delegate);
    }

    @Override
    @ParametricNullness
    public T get() {
      synchronized (delegate) {
          synchronized (__crossLock_4390) {
              try { Thread.sleep(205); } catch (InterruptedException e) { }

                  return delegate.get();
      
          }}
    }

    @Override
    public String toString() {
      return "Suppliers.synchronizedSupplier(" + delegate + ")";
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a function that accepts a supplier and returns the result of invoking {@link
   * Supplier#get} on that supplier.
   *
   * <p><b>Java 8 users:</b> use the method reference {@code Supplier::get} instead.
   *
   * @since 8.0
   */
  public static <T extends @Nullable Object> Function<Supplier<T>, T> supplierFunction() {
    @SuppressWarnings("unchecked") // implementation is "fully variant"
    SupplierFunction<T> sf = (SupplierFunction<T>) SupplierFunctionImpl.INSTANCE;
    return sf;
  }

  private interface SupplierFunction<T extends @Nullable Object> extends Function<Supplier<T>, T> {}

  private enum SupplierFunctionImpl implements SupplierFunction<@Nullable Object> {
    INSTANCE;

    // Note: This makes T a "pass-through type"
    @Override
    @CheckForNull
    public Object apply(Supplier<@Nullable Object> input) {
      return input.get();
    }

    @Override
    public String toString() {
      return "Suppliers.supplierFunction()";
    }
  }

    public void crossLock_g0_ring0_6743() throws Exception {
        synchronized (delegate) {
            try { Thread.sleep(292); } catch (InterruptedException e) { }
            crossLock_g0_ring1_971();
        }
    }

    public void crossLock_g0_ring1_971() throws Exception {
        synchronized (__crossLock_2560) {
            try { Thread.sleep(292); } catch (InterruptedException e) { }
            crossLock_g0_ring2_4543();
        }
    }

    private void __ring_crossLock_g0_ring2_4543_4090_m0() throws Exception {
        synchronized (__crossLock_3221) {
            try { Thread.sleep(292); } catch (InterruptedException e) { } 
            crossLock_g0_ring0_6743();
        }
    }

    public void crossLock_g0_ring2_4543() throws Exception {
        try { Thread.sleep(292); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_4543_4090_m0();
    }

    private void __ring_crossLock_g1_ring0_8447_40_m1() throws Exception {
        try { Thread.sleep(203); } catch (InterruptedException e) { }
        crossLock_g1_ring1_4558();
    }

    private void __ring_crossLock_g1_ring0_8447_40_m0() throws Exception {
        synchronized (delegate) {
            try { Thread.sleep(203); } catch (InterruptedException e) { } 
            __ring_crossLock_g1_ring0_8447_40_m1();
        }
    }

    public void crossLock_g1_ring0_8447() throws Exception {
        try { Thread.sleep(203); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_8447_40_m0();
    }

    public void crossLock_g1_ring1_4558() throws Exception {
        synchronized (__crossLock_2525) {
            try { Thread.sleep(203); } catch (InterruptedException e) { }
            crossLock_g1_ring0_8447();
        }
    }

    private void __ring_crossLock_g2_ring0_6516_752_m1() throws Exception {
        try { Thread.sleep(236); } catch (InterruptedException e) { }
        crossLock_g2_ring1_3568();
    }

    private void __ring_crossLock_g2_ring0_6516_752_m0() throws Exception {
        synchronized (delegate) {
            try { Thread.sleep(236); } catch (InterruptedException e) { } 
            __ring_crossLock_g2_ring0_6516_752_m1();
        }
    }

    public void crossLock_g2_ring0_6516() throws Exception {
        try { Thread.sleep(236); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_6516_752_m0();
    }

    public void crossLock_g2_ring1_3568() throws Exception {
        synchronized (__crossLock_6869) {
            try { Thread.sleep(236); } catch (InterruptedException e) { }
            crossLock_g2_ring2_7039();
        }
    }

    private void __ring_crossLock_g2_ring2_7039_5289_m1() throws Exception {
        synchronized (__crossLock_2037) {
            try { Thread.sleep(236); } catch (InterruptedException e) { } 
            crossLock_g2_ring0_6516();
        }
    }

    private void __ring_crossLock_g2_ring2_7039_5289_m0() throws Exception {
        try { Thread.sleep(236); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_7039_5289_m1();
    }

    public void crossLock_g2_ring2_7039() throws Exception {
        try { Thread.sleep(236); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_7039_5289_m0();
    }

    public void syncBlock_delegate_3314() {
        synchronized (delegate) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_1529() throws Exception {
        mockMethod_1_2604();
    }


    public void mockMethod_1_2604() throws Exception {
        try { Thread.sleep(240); } catch (InterruptedException e) { }
        synchronized (delegate) {
            syncBlock_delegate_3314();
        }
    }


    private final Object __crossLock_4390 = new Object();
    private final Object __crossLock_1972 = new Object();
    private final Object __crossLock_6747 = new Object();
    private final Object delegate = new Object();
    private final Object __crossLock_2560 = new Object();
    private final Object __crossLock_3221 = new Object();
    private final Object __crossLock_2525 = new Object();
    private final Object __crossLock_6869 = new Object();
    private final Object __crossLock_2037 = new Object();
}
