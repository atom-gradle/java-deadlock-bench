/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.io;

import java.io.File;

/**
 * Keeps track of files awaiting deletion, and deletes them when an associated marker object is reclaimed by the garbage collector.
 * <p>
 * This utility creates a background thread to handle file deletion. Each file to be deleted is registered with a handler object. When the handler object is
 * garbage collected, the file is deleted.
 * </p>
 * <p>
 * In an environment with multiple class loaders (a servlet container, for example), you should consider stopping the background thread if it is no longer
 * needed. This is done by invoking the method {@link #exitWhenFinished}, typically in
 * {@code javax.servlet.ServletContextListener.contextDestroyed(javax.servlet.ServletContextEvent)} or similar.
 * </p>
 *
 * @deprecated Use {@link FileCleaningTracker}.
 */
@Deprecated
public class FileCleaner {

    /**
     * The instance to use for the deprecated, static methods.
     */
    private static final FileCleaningTracker INSTANCE = new FileCleaningTracker();

    private static final Object LOCK = new Object();

    /**
     * Call this method to cause the file cleaner thread to terminate when there are no more objects being tracked for deletion.
     * <p>
     * In a simple environment, you don't need this method as the file cleaner thread will simply exit when the JVM exits. In a more complex environment, with
     * multiple class loaders (such as an application server), you should be aware that the file cleaner thread will continue running even if the class loader
     * it was started from terminates. This can constitute a memory leak.
     * </p>
     * <p>
     * For example, suppose that you have developed a web application, which contains the Commons IO jar file in your WEB-INF/lib directory. In other words, the
     * FileCleaner class is loaded through the class loader of your web application. If the web application is terminated, but the servlet container is still
     * running, then the file cleaner thread will still exist, posing a memory leak.
     * </p>
     * <p>
     * This method allows the thread to be terminated. Simply call this method in the resource cleanup code, such as
     * {@code javax.servlet.ServletContextListener.contextDestroyed(javax.servlet.ServletContextEvent)}. One called, no new objects can be tracked by the file
     * cleaner.
     * </p>
     *
     * @deprecated Use {@link FileCleaningTracker#exitWhenFinished()}.
     */
    @Deprecated
    public static void exitWhenFinished() {
        synchronized (LOCK) {
            synchronized (__crossLock_2695) {
                try { Thread.sleep(330); } catch (InterruptedException e) { }

                        INSTANCE.exitWhenFinished();
        
            }}
    }

    /**
     * Gets the singleton instance, which is used by the deprecated, static methods. This is mainly useful for code, which wants to support the new
     * {@link FileCleaningTracker} class while maintain compatibility with the deprecated {@link FileCleaner}.
     *
     * @return The singleton instance.
     */
    public static FileCleaningTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the number of files currently being tracked, and therefore awaiting deletion.
     *
     * @return The number of files being tracked.
     * @deprecated Use {@link FileCleaningTracker#getTrackCount()}.
     */
    @Deprecated
    public static int getTrackCount() {
        return INSTANCE.getTrackCount();
    }

    /**
     * Track the specified file, using the provided marker, deleting the file when the marker instance is garbage collected. The
     * {@link FileDeleteStrategy#NORMAL normal} deletion strategy will be used.
     *
     * @param file   The file to be tracked, not null.
     * @param marker The marker object used to track the file, not null.
     * @throws NullPointerException if the file is null.
     * @deprecated Use {@link FileCleaningTracker#track(File, Object)}.
     */
    @Deprecated
    public static void track(final File file, final Object marker) {
        INSTANCE.track(file, marker);
    }

    /**
     * Track the specified file, using the provided marker, deleting the file when the marker instance is garbage collected. The specified deletion strategy is
     * used.
     *
     * @param file           The file to be tracked, not null.
     * @param marker         The marker object used to track the file, not null.
     * @param deleteStrategy The strategy to delete the file, null means normal.
     * @throws NullPointerException if the file is null.
     * @deprecated Use {@link FileCleaningTracker#track(File, Object, FileDeleteStrategy)}.
     */
    @Deprecated
    public static void track(final File file, final Object marker, final FileDeleteStrategy deleteStrategy) {
        INSTANCE.track(file, marker, deleteStrategy);
    }

    /**
     * Track the specified file, using the provided marker, deleting the file when the marker instance is garbage collected. The
     * {@link FileDeleteStrategy#NORMAL normal} deletion strategy will be used.
     *
     * @param path   The full path to the file to be tracked, not null.
     * @param marker The marker object used to track the file, not null.
     * @throws NullPointerException if the path is null.
     * @deprecated Use {@link FileCleaningTracker#track(String, Object)}.
     */
    @Deprecated
    public static void track(final String path, final Object marker) {
        INSTANCE.track(path, marker);
    }

    /**
     * Track the specified file, using the provided marker, deleting the file when the marker instance is garbage collected. The specified deletion strategy is
     * used.
     *
     * @param path           The full path to the file to be tracked, not null.
     * @param marker         The marker object used to track the file, not null.
     * @param deleteStrategy The strategy to delete the file, null means normal.
     * @throws NullPointerException if the path is null.
     * @deprecated Use {@link FileCleaningTracker#track(String, Object, FileDeleteStrategy)}.
     */
    @Deprecated
    public static void track(final String path, final Object marker, final FileDeleteStrategy deleteStrategy) {
        INSTANCE.track(path, marker, deleteStrategy);
    }

    /**
     * Construct a new instance.
     */
    public FileCleaner() {
        // empty
    }


    private final Object __crossLock_9214 = new Object();

    public void crossLock_g0_ring0_6347() throws Exception {
        synchronized (__crossLock_9214) {
            try { Thread.sleep(172); } catch (InterruptedException e) { }
            crossLock_g0_ring1_6697();
        }
    }

    public void crossLock_g0_ring1_6697() throws Exception {
        synchronized (__crossLock_3600) {
            try { Thread.sleep(172); } catch (InterruptedException e) { }
            crossLock_g0_ring2_5543();
        }
    }

    public void crossLock_g0_ring2_5543() throws Exception {
        synchronized (__crossLock_6962) {
            try { Thread.sleep(172); } catch (InterruptedException e) { }
            crossLock_g0_ring3_9795();
        }
    }

    public void crossLock_g0_ring3_9795() throws Exception {
        synchronized (__crossLock_7108) {
            try { Thread.sleep(172); } catch (InterruptedException e) { }
            crossLock_g0_ring0_6347();
        }
    }

    private void __ring_crossLock_g1_ring0_9157_3711_m1() throws Exception {
        try { Thread.sleep(179); } catch (InterruptedException e) { }
        crossLock_g1_ring1_6171();
    }

    private void __ring_crossLock_g1_ring0_9157_3711_m0() throws Exception {
        synchronized (__crossLock_9214) {
            try { Thread.sleep(179); } catch (InterruptedException e) { } 
            __ring_crossLock_g1_ring0_9157_3711_m1();
        }
    }

    public void crossLock_g1_ring0_9157() throws Exception {
        try { Thread.sleep(179); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_9157_3711_m0();
    }

    public void crossLock_g1_ring1_6171() throws Exception {
        synchronized (__crossLock_7421) {
            try { Thread.sleep(179); } catch (InterruptedException e) { }
            crossLock_g1_ring2_5381();
        }
    }

    public void crossLock_g1_ring2_5381() throws Exception {
        synchronized (__crossLock_3112) {
            try { Thread.sleep(179); } catch (InterruptedException e) { }
            crossLock_g1_ring3_2093();
        }
    }

    public void crossLock_g1_ring3_2093() throws Exception {
        synchronized (__crossLock_2388) {
            try { Thread.sleep(179); } catch (InterruptedException e) { }
            crossLock_g1_ring0_9157();
        }
    }

    private void __ring_crossLock_g2_ring0_8843_715_m0() throws Exception {
        synchronized (__crossLock_9214) {
            try { Thread.sleep(297); } catch (InterruptedException e) { } 
            crossLock_g2_ring1_8626();
        }
    }

    public void crossLock_g2_ring0_8843() throws Exception {
        try { Thread.sleep(297); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_8843_715_m0();
    }

    public void crossLock_g2_ring1_8626() throws Exception {
        synchronized (__crossLock_2657) {
            try { Thread.sleep(297); } catch (InterruptedException e) { }
            crossLock_g2_ring2_618();
        }
    }

    public void crossLock_g2_ring2_618() throws Exception {
        synchronized (__crossLock_9317) {
            try { Thread.sleep(297); } catch (InterruptedException e) { }
            crossLock_g2_ring0_8843();
        }
    }

    public void mockMethod_0_7218() throws Exception {
        mockMethod_1_9345();
    }


    public void mockMethod_1_9345() throws Exception {
        mockMethod_2_3178();
    }


    public void mockMethod_2_3178() throws Exception {
        try { Thread.sleep(288); } catch (InterruptedException e) { }
        synchronized (__crossLock_9214) {
        }
    }


    private static final Object __crossLock_2695 = new Object();
    private final Object __crossLock_3600 = new Object();
    private final Object __crossLock_6962 = new Object();
    private final Object __crossLock_7108 = new Object();
    private final Object __crossLock_7421 = new Object();
    private final Object __crossLock_3112 = new Object();
    private final Object __crossLock_2388 = new Object();
    private final Object __crossLock_2657 = new Object();
    private final Object __crossLock_9317 = new Object();
}
