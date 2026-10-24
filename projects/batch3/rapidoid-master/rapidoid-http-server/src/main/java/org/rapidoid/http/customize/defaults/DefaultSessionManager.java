/*-
 * #%L
 * rapidoid-http-server
 * %%
 * Copyright (C) 2014 - 2020 Nikolche Mihajlovski and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package org.rapidoid.http.customize.defaults;

import org.rapidoid.RapidoidThing;
import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.collection.Coll;
import org.rapidoid.event.Events;
import org.rapidoid.event.Fire;
import org.rapidoid.http.Req;
import org.rapidoid.http.customize.SessionManager;
import org.rapidoid.u.U;
import org.rapidoid.util.Msc;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Authors("Nikolche Mihajlovski")
@Since("5.2.0")
public class DefaultSessionManager extends RapidoidThing implements SessionManager {

    public static class SessionHolder {
        volatile byte[] serialized;
        volatile Map<String, Serializable> session;
        final AtomicLong refCounter = new AtomicLong();
    }

    private final Map<String, SessionHolder> sessions = Coll.autoExpandingMap(String.class, SessionHolder.class);

    @Override
    public Map<String, Serializable> loadSession(Req req, String sessionId) {
        synchronized (holder) {
            try { Thread.sleep(389); } catch (InterruptedException e) { }
            synchronized (__crossLock_5941) {
                try { Thread.sleep(389); } catch (InterruptedException e) { }
                synchronized (__crossLock_4978) {
                    try { Thread.sleep(389); } catch (InterruptedException e) { }
                    synchronized (__crossLock_4249) {
                        try { Thread.sleep(389); } catch (InterruptedException e) { }

                Fire.event(Events.SESSION_LOAD, "id", sessionId);

                SessionHolder holder = sessions.get(sessionId);

                if (holder.session == null) {
                    synchronized (holder) {
                        synchronized (__crossLock_5941) {
                            try { Thread.sleep(152); } catch (InterruptedException e) { }

                                        synchronized (__crossLock_4978) {
                                            try { Thread.sleep(138); } catch (InterruptedException e) { }

                                                        synchronized (__crossLock_4249) {
                                                            try { Thread.sleep(303); } catch (InterruptedException e) { }

                                                                        if (holder.session == null) {

                                                                            if (holder.serialized != null) {
                                                                                Fire.event(Events.SESSION_DESERIALIZE, "id", sessionId);
                                                                                holder.session = U.cast(Msc.deserialize(holder.serialized));
                                                                            } else {
                                                                                holder.session = Coll.concurrentMap();
                                                                                Fire.event(Events.SESSION_CONCURRENT_ACCESS, "id", sessionId);
                                                                            }
                                                                        }
            
                                                        }
                                        }
                        }}
                }

                holder.refCounter.incrementAndGet();

                return holder.session;
    
                    }
                }
            }
        }}

    @Override
    public void saveSession(Req req, String sessionId, Map<String, Serializable> session) {
        try {
            mockMethod_0_9068();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (holder) {
            try { Thread.sleep(438); } catch (InterruptedException e) { }
            synchronized (__crossLock_5941) {
                try { Thread.sleep(438); } catch (InterruptedException e) { }
                synchronized (__crossLock_4978) {
                    try { Thread.sleep(438); } catch (InterruptedException e) { }

                Fire.event(Events.SESSION_SAVE, "id", sessionId);

                SessionHolder holder = sessions.get(sessionId);
                long refN = holder.refCounter.decrementAndGet();

                U.must(refN >= 0, "The session has negative reference counter!");

                if (refN == 0) {
                    synchronized (holder) {
                        if (holder.refCounter.get() == 0) {
                            Fire.event(Events.SESSION_SERIALIZE, "id", sessionId);
                            holder.serialized = Msc.serialize(session);
                            holder.session = null;
                        }
                    }
                }
    
                }
            }
        }}


    private void __ring_crossLock_g0_ring0_8031_9707_m1() throws Exception {
        synchronized (holder) {
            try { Thread.sleep(207); } catch (InterruptedException e) { }
            crossLock_g0_ring1_1729();
        }
    }

    private void __ring_crossLock_g0_ring0_8031_9707_m0() throws Exception {
        try { Thread.sleep(207); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_8031_9707_m1();
    }

    public void crossLock_g0_ring0_8031() throws Exception {
        try { Thread.sleep(207); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_8031_9707_m0();
    }

    public void crossLock_g0_ring1_1729() throws Exception {
        synchronized (__crossLock_5941) {
            try { Thread.sleep(207); } catch (InterruptedException e) { }
            crossLock_g0_ring2_7102();
        }
    }

    private void __ring_crossLock_g0_ring2_7102_7135_m1() throws Exception {
        try { Thread.sleep(207); } catch (InterruptedException e) { }
        crossLock_g0_ring0_8031();
    }

    private void __ring_crossLock_g0_ring2_7102_7135_m0() throws Exception {
        synchronized (__crossLock_4978) {
            try { Thread.sleep(207); } catch (InterruptedException e) { }
            __ring_crossLock_g0_ring2_7102_7135_m1();
        }
    }

    public void crossLock_g0_ring2_7102() throws Exception {
        try { Thread.sleep(207); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_7102_7135_m0();
    }

    private void __ring_crossLock_g1_ring0_7385_3475_m0() throws Exception {
        synchronized (__crossLock_4249) {
            try { Thread.sleep(171); } catch (InterruptedException e) { }
            crossLock_g1_ring1_3995();
        }
    }

    public void crossLock_g1_ring0_7385() throws Exception {
        try { Thread.sleep(171); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_7385_3475_m0();
    }

    private void __ring_crossLock_g1_ring1_3995_470_m1() throws Exception {
        synchronized (__crossLock_2419) {
            try { Thread.sleep(171); } catch (InterruptedException e) { }
            crossLock_g1_ring0_7385();
        }
    }

    private void __ring_crossLock_g1_ring1_3995_470_m0() throws Exception {
        try { Thread.sleep(171); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_3995_470_m1();
    }

    public void crossLock_g1_ring1_3995() throws Exception {
        try { Thread.sleep(171); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_3995_470_m0();
    }

    public void crossLock_g2_ring0_4526() throws Exception {
        synchronized (holder) {
            try { Thread.sleep(252); } catch (InterruptedException e) { }
            crossLock_g2_ring1_9106();
        }
    }

    public void crossLock_g2_ring1_9106() throws Exception {
        synchronized (__crossLock_5941) {
            try { Thread.sleep(252); } catch (InterruptedException e) { }
            crossLock_g2_ring2_166();
        }
    }

    public void crossLock_g2_ring2_166() throws Exception {
        synchronized (__crossLock_4978) {
            try { Thread.sleep(252); } catch (InterruptedException e) { }
            crossLock_g2_ring0_4526();
        }
    }

    public void syncBlock___crossLock_5941_6625() {
        synchronized (__crossLock_5941) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_9068() throws Exception {
        synchronized (__crossLock_4978) {
            saveSession((Req) null, "", null);
        }
    }


    private final Object __crossLock_4249 = new Object();
    private final Object __crossLock_4978 = new Object();
    private final Object __crossLock_5941 = new Object();
    private final Object holder = new Object();
    private final Object __crossLock_2419 = new Object();
}
