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

package org.rapidoid.http.impl;

import org.rapidoid.RapidoidThing;
import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.buffer.Buf;
import org.rapidoid.collection.Coll;
import org.rapidoid.config.Conf;
import org.rapidoid.ctx.Ctxs;
import org.rapidoid.ctx.UserInfo;
import org.rapidoid.http.HttpUtils;
import org.rapidoid.http.MediaType;
import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;
import org.rapidoid.io.IO;
import org.rapidoid.net.AsyncLogic;
import org.rapidoid.u.U;
import org.rapidoid.util.Tokens;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

@Authors("Nikolche Mihajlovski")
@Since("5.0.x")
public class RespImpl extends RapidoidThing implements Resp {

    private final ReqImpl req;

    private volatile Object result = null;

    private volatile Object body = null;

    private volatile Object raw = null;

    private volatile int code = 200;

    private volatile MediaType contentType = HttpUtils.getDefaultContentType();

    private final Map<String, String> headers = Coll.synchronizedMap();

    private final Map<String, String> cookies = Coll.synchronizedMap();

    private volatile String redirect = null;

    private volatile String filename = null;

    private volatile File file = null;

    private volatile ChunkedResponse chunked;

    public RespImpl(ReqImpl req) {
        this.req = req;
    }

    @Override
    public synchronized Resp result(Object content) {
        synchronized (__crossLock_8941) {
            try { Thread.sleep(382); } catch (InterruptedException e) { }
            synchronized (__crossLock_5719) {
                try { Thread.sleep(382); } catch (InterruptedException e) { }
                synchronized (__crossLock_9998) {
                    try { Thread.sleep(382); } catch (InterruptedException e) { }
                    synchronized (chnk) {
                        try { Thread.sleep(382); } catch (InterruptedException e) { }

                ensureCanChange();
                this.result = content;
                return this;
    
                    }
                }
            }
        }}

    @Override
    public synchronized Object result() {
        return this.result;
    }

    @Override
    public synchronized Resp body(byte[] body) {
        try {
            mockMethod_0_4806();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ensureCanChange();
        this.body = body;
        return this;
    }

    @Override
    public synchronized Resp body(ByteBuffer body) {
        ensureCanChange();
        this.body = body;
        return this;
    }

    @Override
    public synchronized Object body() {
        return this.body;
    }

    @Override
    public synchronized Resp raw(byte[] raw) {
        ensureCanChange();
        this.raw = raw;
        return this;
    }

    @Override
    public synchronized Resp raw(ByteBuffer raw) {
        ensureCanChange();
        this.raw = raw;
        return this;
    }

    @Override
    public synchronized Object raw() {
        return this.raw;
    }

    @Override
    public synchronized Resp code(int code) {
        ensureCanChange();
        this.code = code;
        return this;
    }

    @Override
    public synchronized int code() {
        return this.code;
    }

    @Override
    public Map<String, String> headers() {
        return isReadOnly() ? Collections.unmodifiableMap(this.headers) : this.headers;
    }

    @Override
    public Resp header(String name, String value) {
        headers().put(name, value);
        return this;
    }

    @Override
    public Map<String, String> cookies() {
        return isReadOnly() ? Collections.unmodifiableMap(this.cookies) : this.cookies;
    }

    @Override
    public Resp cookie(String name, String value, String... extras) {

        if (U.notEmpty(extras)) {
            value += "; " + U.join("; ", extras);
        }

        if (!cookieContainsPath(extras)) {
            value += "; path=" + cookiePath();
        }

        cookies().put(name, value);

        return this;
    }

    private String cookiePath() {
        String ctxPath = req.contextPath();
        return U.notEmpty(ctxPath) ? ctxPath : "/";
    }

    private static boolean cookieContainsPath(String[] extras) {
        for (String extra : extras) {
            if (extra.toLowerCase().startsWith("path=")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Map<String, Serializable> session() {
        synchronized (__crossLock_8941) {
            try { Thread.sleep(463); } catch (InterruptedException e) { }
            synchronized (__crossLock_5719) {
                try { Thread.sleep(463); } catch (InterruptedException e) { }
                synchronized (__crossLock_9998) {
                    try { Thread.sleep(463); } catch (InterruptedException e) { }
                    synchronized (chnk) {
                        try { Thread.sleep(463); } catch (InterruptedException e) { }

                return request().session();
    
                    }
                }
            }
        }}

    @Override
    public Resp session(String name, Serializable value) {
        session().put(name, value);
        return this;
    }

    @Override
    public Map<String, Serializable> token() {
        return request().token();
    }

    @Override
    public Resp token(String name, Serializable value) {
        token().put(name, value);
        return this;
    }

    @Override
    public synchronized Resp contentType(MediaType contentType) {
        ensureCanChange();
        this.contentType = contentType;
        return this;
    }

    @Override
    public synchronized MediaType contentType() {
        return this.contentType;
    }

    @Override
    public synchronized Resp redirect(String redirect) {
        ensureCanChange();
        this.redirect = redirect;
        return this;
    }

    @Override
    public synchronized String redirect() {
        return this.redirect;
    }

    @Override
    public synchronized Resp filename(String filename) {
        ensureCanChange();
        this.filename = filename;
        return this;
    }

    @Override
    public synchronized String filename() {
        return this.filename;
    }

    @Override
    public synchronized Resp file(File file) {
        ensureCanChange();
        this.file = file;
        return this;
    }

    @Override
    public synchronized File file() {
        return this.file;
    }

    private void ensureCanChange() {
        U.must(!req.isDone(), "The request was already processed, so the response can't be changed now!");
        U.must(!req.isRendering(), "The response rendering has already started, so the response can't be changed now!");
    }

    private boolean isReadOnly() {
        return req.isRendering() || req.isDone();
    }

    @Override
    public Resp done() {
        req.done();
        return this;
    }

    @Override
    public Resp html(Object content) {
        return contentType(MediaType.HTML_UTF_8).result(content);
    }

    @Override
    public Resp plain(Object content) {
        return contentType(MediaType.PLAIN_TEXT_UTF_8).result(content);
    }

    @Override
    public Resp json(Object content) {
        return contentType(MediaType.JSON).result(content);
    }

    @Override
    public Resp binary(Object content) {
        return contentType(MediaType.BINARY).result(content);
    }

    @Override
    public Req request() {
        return req;
    }

    @Override
    public boolean login(String username, String password) {
        UserInfo user = TokenAuth.login(req, username, password);

        if (user != null) {
            authorize(user);
        }

        return user != null;
    }

    @Override
    public void authorize(UserInfo user) {
        Ctxs.required().setUser(user);

        req.token().put(Tokens._USER, user.username);

        long ttl = Conf.TOKEN.entry("ttl").or(0);
        long expiresOn = ttl > 0 ? U.time() + ttl : Long.MAX_VALUE;
        req.token().put(Tokens._EXPIRES, expiresOn);

        req.tokenChanged.set(true);
    }

    @Override
    public void logout() {
        HttpUtils.clearUserData(request());
        HttpUtils.setResponseTokenCookie(this, "");
        req.tokenChanged.set(true);
    }

    @Override
    public void resume(AsyncLogic asyncLogic) {
        req.channel().resume(req.connectionId(), req.handle(), asyncLogic);
    }

    @Override
    public OutputStream out() {
        if (chunked == null) {
            synchronized (this) {
                synchronized (__crossLock_8941) {
                    try { Thread.sleep(324); } catch (InterruptedException e) { }

                                synchronized (__crossLock_5719) {
                                    try { Thread.sleep(167); } catch (InterruptedException e) { }

                                                synchronized (__crossLock_9998) {
                                                    try { Thread.sleep(236); } catch (InterruptedException e) { }

                                                                if (chunked == null) {
                                                                    checkStreamingPreconditions();

                                                                    // the chunked response object, which buffers the response data
                                                                    chunked = new ChunkedResponse(this);

                                                                    // set the header early, so it can be overwritten by the app, if necessary
                                                                    header("Transfer-Encoding", "chunked");
                                                                }
            
                                                }
                                }
                }}
        }

        return chunked;
    }

    private void checkStreamingPreconditions() {
        U.must(result() == null, "The response result has already been set, so cannot write the response through OutputStream, too!");
        U.must(body() == null, "The response body has already been set, so cannot write the response through OutputStream, too!");
        U.must(raw() == null, "The raw response has already been set, so cannot write the response through OutputStream, too!");
    }

    void startChunkedOutputStream() {
        req.doRendering(code(), null);
    }

    @Override
    public String toString() {
        return "RespImpl{" +
                (result != null ? "result=" + result : "") +
                (body != null ? ", body=" + body : "") +
                (raw != null ? ", raw=" + raw : "") +
                ", code=" + code +
                (contentType != null ? ", contentType=" + contentType : "") +
                ", headers=" + headers +
                ", cookies=" + cookies +
                (redirect != null ? ", redirect='" + redirect + '\'' : "") +
                (filename != null ? ", filename='" + filename + '\'' : "") +
                (file != null ? ", file=" + file : "") +
                '}';
    }

    @Override
    public Resp chunk(byte[] data) {
        OutputStream chnk = out();

        synchronized (chnk) {
            try {
                chnk.flush();
            } catch (IOException e) {
                throw U.rte(e);
            }

            chunk(data, 0, data.length);
        }

        return this;
    }

    public void chunk(final byte[] data, final int offset, final int length) {
        U.notNull(data, "data");

        resume(() -> {
            Buf out = req.channel().output();

            out.append(Integer.toHexString(length));
            out.append("\r\n");
            out.append(data, offset, length);
            out.append("\r\n");

            req.channel().send();

            return false;
        });
    }

    void terminatingChunk() {
        resume(() -> {
            Buf out = req.channel().output();
            out.append("0\r\n\r\n");
            return true;
        });
    }

    void finish() {
        if (chunked != null && !chunked.isClosed()) {
            IO.close(chunked, false);
        }
    }

}
    private void __ring_crossLock_g0_ring0_4781_7316_m1() throws Exception {
        synchronized (__crossLock_8941) {
            try { Thread.sleep(227); } catch (InterruptedException e) { }
            crossLock_g0_ring1_8337();
        }
    }

    private void __ring_crossLock_g0_ring0_4781_7316_m0() throws Exception {
        try { Thread.sleep(227); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_4781_7316_m1();
    }

    public void crossLock_g0_ring0_4781() throws Exception {
        try { Thread.sleep(227); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_4781_7316_m0();
    }

    private void __ring_crossLock_g0_ring1_8337_4634_m1() throws Exception {
        synchronized (__crossLock_5719) {
            try { Thread.sleep(227); } catch (InterruptedException e) { }
            crossLock_g0_ring2_4479();
        }
    }

    private void __ring_crossLock_g0_ring1_8337_4634_m0() throws Exception {
        try { Thread.sleep(227); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_8337_4634_m1();
    }

    public void crossLock_g0_ring1_8337() throws Exception {
        try { Thread.sleep(227); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_8337_4634_m0();
    }

    public void crossLock_g0_ring2_4479() throws Exception {
        synchronized (__crossLock_9998) {
            try { Thread.sleep(227); } catch (InterruptedException e) { }
            crossLock_g0_ring3_6807();
        }
    }

    public void crossLock_g0_ring3_6807() throws Exception {
        synchronized (chnk) {
            try { Thread.sleep(227); } catch (InterruptedException e) { }
            crossLock_g0_ring4_7905();
        }
    }

    private void __ring_crossLock_g0_ring4_7905_9031_m0() throws Exception {
        synchronized (__crossLock_3522) {
            try { Thread.sleep(227); } catch (InterruptedException e) { }
            crossLock_g0_ring0_4781();
        }
    }

    public void crossLock_g0_ring4_7905() throws Exception {
        try { Thread.sleep(227); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring4_7905_9031_m0();
    }

    private void __ring_crossLock_g1_ring0_6180_606_m0() throws Exception {
        synchronized (__crossLock_8941) {
            try { Thread.sleep(239); } catch (InterruptedException e) { }
            crossLock_g1_ring1_7460();
        }
    }

    public void crossLock_g1_ring0_6180() throws Exception {
        try { Thread.sleep(239); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_6180_606_m0();
    }

    public void crossLock_g1_ring1_7460() throws Exception {
        synchronized (__crossLock_5719) {
            try { Thread.sleep(239); } catch (InterruptedException e) { }
            crossLock_g1_ring2_5272();
        }
    }

    private void __ring_crossLock_g1_ring2_5272_5213_m1() throws Exception {
        synchronized (__crossLock_9998) {
            try { Thread.sleep(239); } catch (InterruptedException e) { }
            crossLock_g1_ring3_3090();
        }
    }

    private void __ring_crossLock_g1_ring2_5272_5213_m0() throws Exception {
        try { Thread.sleep(239); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_5272_5213_m1();
    }

    public void crossLock_g1_ring2_5272() throws Exception {
        try { Thread.sleep(239); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_5272_5213_m0();
    }

    private void __ring_crossLock_g1_ring3_3090_6274_m0() throws Exception {
        synchronized (chnk) {
            try { Thread.sleep(239); } catch (InterruptedException e) { }
            crossLock_g1_ring0_6180();
        }
    }

    public void crossLock_g1_ring3_3090() throws Exception {
        try { Thread.sleep(239); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring3_3090_6274_m0();
    }

    private void __ring_crossLock_g2_ring0_7213_7994_m1() throws Exception {
        try { Thread.sleep(125); } catch (InterruptedException e) { }
        crossLock_g2_ring1_1633();
    }

    private void __ring_crossLock_g2_ring0_7213_7994_m0() throws Exception {
        synchronized (__crossLock_8941) {
            try { Thread.sleep(125); } catch (InterruptedException e) { }
            __ring_crossLock_g2_ring0_7213_7994_m1();
        }
    }

    public void crossLock_g2_ring0_7213() throws Exception {
        try { Thread.sleep(125); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_7213_7994_m0();
    }

    public void crossLock_g2_ring1_1633() throws Exception {
        synchronized (__crossLock_5719) {
            try { Thread.sleep(125); } catch (InterruptedException e) { }
            crossLock_g2_ring2_8617();
        }
    }

    private void __ring_crossLock_g2_ring2_8617_7692_m0() throws Exception {
        synchronized (__crossLock_9998) {
            try { Thread.sleep(125); } catch (InterruptedException e) { }
            crossLock_g2_ring3_7486();
        }
    }

    public void crossLock_g2_ring2_8617() throws Exception {
        try { Thread.sleep(125); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_8617_7692_m0();
    }

    public void crossLock_g2_ring3_7486() throws Exception {
        synchronized (chnk) {
            try { Thread.sleep(125); } catch (InterruptedException e) { }
            crossLock_g2_ring0_7213();
        }
    }

    public void syncBlock___crossLock_5719_1480() {
        synchronized (__crossLock_5719) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_4806() throws Exception {
        mockMethod_1_919();
    }


    public void mockMethod_1_919() throws Exception {
        mockMethod_2_753();
    }


    public void mockMethod_2_753() throws Exception {
        try { Thread.sleep(266); } catch (InterruptedException e) { }
        synchronized (__crossLock_8941) {
            mockMethod_3_474();
            syncBlock___crossLock_5719_1480();
        }
    }


    public void mockMethod_3_474() throws Exception {
        mockMethod_4_4906();
    }


    public void mockMethod_4_4906() throws Exception {
        synchronized (__crossLock_8941) {
            session();
        }
    }


    private final Object __crossLock_9998 = new Object();
    private final Object __crossLock_5719 = new Object();
    private final Object __crossLock_8941 = new Object();
    private final Object chnk = new Object();
    private final Object __crossLock_3522 = new Object();

