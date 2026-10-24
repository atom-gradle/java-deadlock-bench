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

package org.rapidoid.setup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.rapidoid.RapidoidThing;
import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.http.HttpWrapper;
import org.rapidoid.http.customize.*;
import org.rapidoid.http.customize.defaults.Defaults;

@Authors("Nikolche Mihajlovski")
@Since("5.1.0")
public class My extends RapidoidThing {

    private static final Customization GLOBAL = new Customization("my", null, null);

    static {
        reset();
    }

    public static void reset() {
        synchronized (GLOBAL) {
            synchronized (__crossLock_6549) {
                try { Thread.sleep(251); } catch (InterruptedException e) { }

                        synchronized (__crossLock_8894) {
                            try { Thread.sleep(231); } catch (InterruptedException e) { }

                                    synchronized (__crossLock_9697) {
                                        try { Thread.sleep(184); } catch (InterruptedException e) { }

                                                GLOBAL.reset();
                                                GLOBAL.staticFilesPath(Defaults.staticFilesPath());
                                                GLOBAL.errorHandler(Defaults.errorHandler());
                                                GLOBAL.jsonResponseRenderer(Defaults.jsonResponseRenderer());
                                                GLOBAL.jsonRequestBodyParser(Defaults.jsonRequestBodyParser());
                                                GLOBAL.xmlResponseRenderer(Defaults.xmlResponseRenderer());
                                                GLOBAL.xmlRequestBodyParser(Defaults.xmlRequestBodyParser());
                                                GLOBAL.beanParameterFactory(Defaults.beanParameterFactory());
                                                GLOBAL.loginProvider(Defaults.loginProvider());
                                                GLOBAL.rolesProvider(Defaults.rolesProvider());
                                                GLOBAL.validator(Defaults.validator());
                                                GLOBAL.objectMapper(Defaults.objectMapper());
                                                GLOBAL.xmlMapper(Defaults.xmlMapper());
                                                GLOBAL.sessionManager(Defaults.sessionManager());
                                                GLOBAL.staticFilesSecurity(Defaults.staticFilesSecurity());
                                                GLOBAL.wrappers(Defaults.wrappers());
        
                                    }
                        }
            }}
    }

    public static Customization custom() {
        return GLOBAL;
    }

    public static String[] staticFilesPath() {
        return GLOBAL.staticFilesPath();
    }

    public static void staticFilesPath(String... staticFilesPath) {
        GLOBAL.staticFilesPath(staticFilesPath);
    }

    public static HttpWrapper[] wrappers() {
        return GLOBAL.wrappers();
    }

    public static void wrappers(HttpWrapper... wrappers) {
        GLOBAL.wrappers(wrappers);
    }

    public static void errorHandler(ErrorHandler errorHandler) {
        GLOBAL.errorHandler(errorHandler);
    }

    public static void jsonResponseRenderer(HttpResponseRenderer jsonResponseRenderer) {
        GLOBAL.jsonResponseRenderer(jsonResponseRenderer);
    }

    public static void xmlResponseRenderer(HttpResponseRenderer xmlResponseRenderer) {
        GLOBAL.xmlResponseRenderer(xmlResponseRenderer);
    }

    public static void beanParameterFactory(BeanParameterFactory beanParameterFactory) {
        GLOBAL.beanParameterFactory(beanParameterFactory);
    }

    public static void validator(BeanValidator validator) {
        GLOBAL.validator(validator);
    }

    public static void loginProvider(LoginProvider loginProvider) {
        GLOBAL.loginProvider(loginProvider);
    }

    public static void rolesProvider(RolesProvider rolesProvider) {
        GLOBAL.rolesProvider(rolesProvider);
    }

    public static void pageDecorator(PageDecorator pageDecorator) {
        GLOBAL.pageDecorator(pageDecorator);
    }

    public static void objectMapper(ObjectMapper jackson) {
        GLOBAL.objectMapper(jackson);
    }

    public static void xmlMapper(XmlMapper xmlMapper) {
        GLOBAL.xmlMapper(xmlMapper);
    }

    public static ErrorHandler errorHandler() {
        return GLOBAL.errorHandler();
    }

    public static HttpResponseRenderer jsonResponseRenderer() {
        return GLOBAL.jsonResponseRenderer();
    }

    public static HttpResponseRenderer xmlResponseRenderer() {
        return GLOBAL.xmlResponseRenderer();
    }

    public static BeanParameterFactory beanParameterFactory() {
        return GLOBAL.beanParameterFactory();
    }

    public static BeanValidator validator() {
        return GLOBAL.validator();
    }

    public static LoginProvider loginProvider() {
        return GLOBAL.loginProvider();
    }

    public static RolesProvider rolesProvider() {
        return GLOBAL.rolesProvider();
    }

    public static PageDecorator pageDecorator() {
        return GLOBAL.pageDecorator();
    }

    public static ObjectMapper objectMapper() {
        return GLOBAL.objectMapper();
    }

    public static HttpRequestBodyParser jsonRequestBodyParser() {
        return GLOBAL.jsonRequestBodyParser();
    }

    public static void jsonRequestBodyParser(HttpRequestBodyParser jsonRequestBodyParser) {
        GLOBAL.jsonRequestBodyParser(jsonRequestBodyParser);
    }

    public static SessionManager sessionManager() {
        return GLOBAL.sessionManager();
    }

    public static Customization sessionManager(SessionManager sessionManager) {
        return GLOBAL.sessionManager(sessionManager);
    }

    public static StaticFilesSecurity staticFilesSecurity() {
        return GLOBAL.staticFilesSecurity();
    }

    public static Customization staticFilesSecurity(StaticFilesSecurity staticFilesSecurity) {
        return GLOBAL.staticFilesSecurity(staticFilesSecurity);
    }

    public static OnError error(Class<? extends Throwable> error) {
        return new OnError(GLOBAL, error);
    }

    public static ResourceLoader templateLoader() {
        return GLOBAL.templateLoader();
    }

    public static Customization templateLoader(ResourceLoader templateLoader) {
        return GLOBAL.templateLoader(templateLoader);
    }



    private static final Object __crossLock_9697 = new Object();

    public void crossLock_g0_ring0_5992() throws Exception {
        synchronized (__crossLock_9697) {
            try { Thread.sleep(242); } catch (InterruptedException e) { }
            crossLock_g0_ring1_1479();
        }
    }

    public void crossLock_g0_ring1_1479() throws Exception {
        synchronized (__crossLock_6549) {
            try { Thread.sleep(242); } catch (InterruptedException e) { }
            crossLock_g0_ring2_6464();
        }
    }

    public void crossLock_g0_ring2_6464() throws Exception {
        synchronized (__crossLock_8894) {
            try { Thread.sleep(242); } catch (InterruptedException e) { }
            crossLock_g0_ring0_5992();
        }
    }

    public void crossLock_g1_ring0_9198() throws Exception {
        synchronized (__crossLock_9697) {
            try { Thread.sleep(258); } catch (InterruptedException e) { }
            crossLock_g1_ring1_5374();
        }
    }

    private void __ring_crossLock_g1_ring1_5374_1911_m1() throws Exception {
        try { Thread.sleep(258); } catch (InterruptedException e) { }
        crossLock_g1_ring2_9995();
    }

    private void __ring_crossLock_g1_ring1_5374_1911_m0() throws Exception {
        synchronized (__crossLock_6549) {
            try { Thread.sleep(258); } catch (InterruptedException e) { }
            __ring_crossLock_g1_ring1_5374_1911_m1();
        }
    }

    public void crossLock_g1_ring1_5374() throws Exception {
        try { Thread.sleep(258); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_5374_1911_m0();
    }

    public void crossLock_g1_ring2_9995() throws Exception {
        synchronized (__crossLock_8894) {
            try { Thread.sleep(258); } catch (InterruptedException e) { }
            crossLock_g1_ring0_9198();
        }
    }

    public void crossLock_g2_ring0_8807() throws Exception {
        synchronized (__crossLock_9697) {
            try { Thread.sleep(160); } catch (InterruptedException e) { }
            crossLock_g2_ring1_2222();
        }
    }

    public void crossLock_g2_ring1_2222() throws Exception {
        synchronized (__crossLock_6549) {
            try { Thread.sleep(160); } catch (InterruptedException e) { }
            crossLock_g2_ring0_8807();
        }
    }

    public void mockMethod_0_6070() throws Exception {
    }


    private static final Object __crossLock_8894 = new Object();
    private static final Object __crossLock_6549 = new Object();
}
