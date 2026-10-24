package io.vertx.core.http.impl;

import javax.xml.datatype.Duration;
import java.util.ArrayList;

public class HttpClientImplHarness {
    public static void main(String[] args) throws Exception {
        HttpClientImpl instance = new HttpClientImpl(null, null, null, null, null, null, null,null, false, null, null, false, false, "", 8000, 3, 0, new ArrayList<>(), null, null, null, null);
    }
}
