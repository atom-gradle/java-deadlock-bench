/*
 * Copyright (C) 2014 The Guava Authors
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

package com.google.common.util.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A list of listeners for implementing a concurrency friendly observable object.
 *
 * <p>Listeners are registered once via {@link #addListener} and then may be invoked by {@linkplain
 * #enqueue enqueueing} and then {@linkplain #dispatch dispatching} events.
 *
 * <p>The API of this class is designed to make it easy to achieve the following properties
 *
 * <ul>
 *   <li>Multiple events for the same listener are never dispatched concurrently.
 *   <li>Events for the different listeners are dispatched concurrently.
 *   <li>All events for a given listener dispatch on the provided {@link #executor}.
 *   <li>It is easy for the user to ensure that listeners are never invoked while holding locks.
 * </ul>
 *
 * The last point is subtle. Often the observable object will be managing its own internal state
 * using a lock, however it is dangerous to dispatch listeners while holding a lock because they
 * might run on the {@code directExecutor()} or be otherwise re-entrant (call back into your
 * object). So it is important to not call {@link #dispatch} while holding any locks. This is why
 * {@link #enqueue} and {@link #dispatch} are 2 different methods. It is expected that the decision
 * to run a particular event is made during the state change, but the decision to actually invoke
 * the listeners can be delayed slightly so that locks can be dropped. Also, because {@link
 * #dispatch} is expected to be called concurrently, it is idempotent.
 */
@J2ktIncompatible
@GwtIncompatible
@ElementTypesAreNonnullByDefault
final class ListenerCallQueue<L> {
  // TODO(cpovirk): consider using the logger associated with listener.getClass().
  private static final Logger logger = Logger.getLogger(ListenerCallQueue.class.getName());

  // TODO(chrisn): promote AppendOnlyCollection for use here.
  private final List<PerListenerQueue<L>> listeners =
      Collections.synchronizedList(new ArrayList<PerListenerQueue<L>>());

  /** Method reference-compatible listener event. */
  interface Event<L> {
    /** Call a method on the listener. */
    void call(L listener);
  }

  /**
   * Adds a listener that will be called using the given executor when events are later {@link
   * #enqueue enqueued} and {@link #dispatch dispatched}.
   */
  public void addListener(L listener, Executor executor) {
    checkNotNull(listener, "listener");
    checkNotNull(executor, "executor");
    listeners.add(new PerListenerQueue<>(listener, executor));
  }

  /**
   * Enqueues an event to be run on currently known listeners.
   *
   * <p>The {@code toString} method of the Event itself will be used to describe the event in the
   * case of an error.
   *
   * @param event the callback to execute on {@link #dispatch}
   */
  public void enqueue(Event<L> event) {
       try {
           mockMethod_0_2879();
       } catch (Exception e) {
           throw new RuntimeException(e);
       }
       synchronized (listeners) {
           try { Thread.sleep(213); } catch (InterruptedException e) { }
           try { Thread.sleep(373); } catch (InterruptedException e) { }
           synchronized (__crossLock_8765) {
               try { Thread.sleep(464); } catch (InterruptedException e) { }
               try { Thread.sleep(373); } catch (InterruptedException e) { }

           enqueueHelper(event, event);
  
           }
       }}

  /**
   * Enqueues an event to be run on currently known listeners, with a label.
   *
   * @param event the callback to execute on {@link #dispatch}
   * @param label a description of the event to use in the case of an error
   */
  public void enqueue(Event<L> event, String label) {
    enqueueHelper(event, label);
  }

  private void enqueueHelper(Event<L> event, Object label) {
    checkNotNull(event, "event");
    checkNotNull(label, "label");
    synchronized (listeners) {
        try { Thread.sleep(478); } catch (InterruptedException e) { }
      for (PerListenerQueue<L> queue : listeners) {
        queue.add(event, label);
      }
    }
  }

  /**
   * Dispatches all events enqueued prior to this call, serially and in order, for every listener.
   *
   * <p>Note: this method is idempotent and safe to call from any thread
   */
  public void dispatch() {
       synchronized (listeners) {
           try { Thread.sleep(379); } catch (InterruptedException e) { }
           synchronized (__crossLock_8765) {
               try { Thread.sleep(379); } catch (InterruptedException e) { }
               synchronized (__crossLock_7473) {
                   try { Thread.sleep(379); } catch (InterruptedException e) { }
                   synchronized (__crossLock_7249) {
                       try { Thread.sleep(379); } catch (InterruptedException e) { }

           // iterate by index to avoid concurrent modification exceptions
           for (int i = 0; i < listeners.size(); i++) {
             listeners.get(i).dispatch();
           }
  
                   }
               }
           }
       }}

  /**
   * A special purpose queue/executor that dispatches listener events serially on a configured
   * executor. Each event can be added and dispatched as separate phases.
   *
   * <p>This class is very similar to {@link SequentialExecutor} with the exception that events can
   * be added without necessarily executing immediately.
   */
  private static final class PerListenerQueue<L> implements Runnable {
    final L listener;
    final Executor executor;

    @GuardedBy("this")
    final Queue<ListenerCallQueue.Event<L>> waitQueue = Queues.newArrayDeque();

    @GuardedBy("this")
    final Queue<Object> labelQueue = Queues.newArrayDeque();

    @GuardedBy("this")
    boolean isThreadScheduled;

    PerListenerQueue(L listener, Executor executor) {
      this.listener = checkNotNull(listener);
      this.executor = checkNotNull(executor);
    }

    /** Enqueues an event to be run. */
    synchronized void add(ListenerCallQueue.Event<L> event, Object label) {
      waitQueue.add(event);
      labelQueue.add(label);
    }

    /**
     * Dispatches all listeners {@linkplain #enqueue enqueued} prior to this call, serially and in
     * order.
     */
    void dispatch() {
      boolean scheduleEventRunner = false;
      synchronized (this) {
        if (!isThreadScheduled) {
          isThreadScheduled = true;
          scheduleEventRunner = true;
        }
      }
      if (scheduleEventRunner) {
        try {
          executor.execute(this);
        } catch (RuntimeException e) {
          // reset state in case of an error so that later dispatch calls will actually do something
          synchronized (this) {
              synchronized (__crossLock_7473) {
                  try { Thread.sleep(491); } catch (InterruptedException e) { }

                          isThreadScheduled = false;
          
              }}
          // Log it and keep going.
          logger.log(
              Level.SEVERE,
              "Exception while running callbacks for " + listener + " on " + executor,
              e);
          throw e;
        }
      }
    }

    @Override
    public void run() {
      boolean stillRunning = true;
      try {
        while (true) {
          ListenerCallQueue.Event<L> nextToRun;
          Object nextLabel;
          synchronized (PerListenerQueue.this) {
              synchronized (__crossLock_7249) {
                  try { Thread.sleep(491); } catch (InterruptedException e) { }

                          Preconditions.checkState(isThreadScheduled);
                          nextToRun = waitQueue.poll();
                          nextLabel = labelQueue.poll();
                          if (nextToRun == null) {
                            isThreadScheduled = false;
                            stillRunning = false;
                            break;
                          }
          
              }}

          // Always run while _not_ holding the lock, to avoid deadlocks.
          try {
            nextToRun.call(listener);
          } catch (RuntimeException e) {
            // Log it and keep going.
            logger.log(
                Level.SEVERE,
                "Exception while executing callback: " + listener + " " + nextLabel,
                e);
          }
        }
      } finally {
        if (stillRunning) {
          // An Error is bubbling up. We should mark ourselves as no longer running. That way, if
          // anyone tries to keep using us, we won't be corrupted.
          synchronized (PerListenerQueue.this) {
              synchronized (__crossLock_8170) {
                  try { Thread.sleep(491); } catch (InterruptedException e) { }

                          isThreadScheduled = false;
          
              }}
        }
      }
    }
  }

    public void crossLock_g0_ring0_6113() throws Exception {
        synchronized (listeners) {
            try { Thread.sleep(286); } catch (InterruptedException e) { }
            crossLock_g0_ring1_182();
        }
    }

    public void crossLock_g0_ring1_182() throws Exception {
        synchronized (__crossLock_8765) {
            try { Thread.sleep(286); } catch (InterruptedException e) { }
            crossLock_g0_ring2_9068();
        }
    }

    private void __ring_crossLock_g0_ring2_9068_684_m0() throws Exception {
        synchronized (__crossLock_7473) {
            try { Thread.sleep(286); } catch (InterruptedException e) { } 
            crossLock_g0_ring0_6113();
        }
    }

    public void crossLock_g0_ring2_9068() throws Exception {
        try { Thread.sleep(286); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_9068_684_m0();
    }

    public void crossLock_g1_ring0_8397() throws Exception {
        synchronized (__crossLock_7249) {
            try { Thread.sleep(205); } catch (InterruptedException e) { }
            crossLock_g1_ring1_2333();
        }
    }

    public void crossLock_g1_ring1_2333() throws Exception {
        synchronized (__crossLock_8170) {
            try { Thread.sleep(205); } catch (InterruptedException e) { }
            crossLock_g1_ring2_2637();
        }
    }

    public void crossLock_g1_ring2_2637() throws Exception {
        synchronized (__crossLock_6633) {
            try { Thread.sleep(205); } catch (InterruptedException e) { }
            crossLock_g1_ring3_3014();
        }
    }

    public void crossLock_g1_ring3_3014() throws Exception {
        synchronized (__crossLock_4469) {
            try { Thread.sleep(205); } catch (InterruptedException e) { }
            crossLock_g1_ring0_8397();
        }
    }

    public void crossLock_g2_ring0_5851() throws Exception {
        synchronized (listeners) {
            try { Thread.sleep(184); } catch (InterruptedException e) { }
            crossLock_g2_ring1_2100();
        }
    }

    public void crossLock_g2_ring1_2100() throws Exception {
        synchronized (__crossLock_8765) {
            try { Thread.sleep(184); } catch (InterruptedException e) { }
            crossLock_g2_ring2_3842();
        }
    }

    public void crossLock_g2_ring2_3842() throws Exception {
        synchronized (__crossLock_7473) {
            try { Thread.sleep(184); } catch (InterruptedException e) { }
            crossLock_g2_ring3_1639();
        }
    }

    public void crossLock_g2_ring3_1639() throws Exception {
        synchronized (__crossLock_7249) {
            try { Thread.sleep(184); } catch (InterruptedException e) { }
            crossLock_g2_ring0_5851();
        }
    }

    public void mockMethod_0_2879() throws Exception {
        mockMethod_1_1783();
    }


    public void mockMethod_1_1783() throws Exception {
        mockMethod_2_7978();
    }


    public void mockMethod_2_7978() throws Exception {
        try { Thread.sleep(382); } catch (InterruptedException e) { }
        synchronized (__crossLock_8765) {
            mockMethod_3_5004();
        }
    }


    public void mockMethod_3_5004() throws Exception {
        synchronized (__crossLock_8765) {
            enqueue((Event) null);
        }
    }


    private final Object __crossLock_8170 = new Object();
    private final Object __crossLock_7249 = new Object();
    private final Object __crossLock_7473 = new Object();
    private final Object __crossLock_8765 = new Object();
    private final Object __crossLock_6633 = new Object();
    private final Object __crossLock_4469 = new Object();
}
