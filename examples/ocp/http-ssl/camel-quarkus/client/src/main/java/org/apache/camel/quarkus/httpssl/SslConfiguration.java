/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.httpssl;

import jakarta.inject.Named;
import jakarta.inject.Inject;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.camel.support.jsse.KeyManagersParameters;
import jakarta.enterprise.context.ApplicationScoped;

import org.apache.camel.CamelContext;


@ApplicationScoped
public class SslConfiguration {

    @Inject
    CamelContext camelContext;

    @Named(("clientConfig"))
    public SSLContextParameters sslContextParameters() {
        final String keyPassword = camelContext.resolvePropertyPlaceholders("{{secret:http-ssl-example-tls-password/password}}");
        final String keystoreFile = camelContext.resolvePropertyPlaceholders("{{secret-binary:ocp-ssl-client-tls/keystore.jks}}");
        final String truststoreFile = camelContext.resolvePropertyPlaceholders("{{secret-binary:ocp-ssl-camel-server-tls/truststore.jks}}");

        final SSLContextParameters sslContextParameters = new SSLContextParameters();

        //keystore
        final KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource("file:" + keystoreFile);
        ksp.setType("PKCS12");
        final KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyPassword(keyPassword);
        kmp.setKeyStore(ksp);

        //truststore
        final KeyStoreParameters tsp = new KeyStoreParameters();
        tsp.setResource("file:" + truststoreFile);
        tsp.setType("PKCS12");
        final TrustManagersParameters tsm = new TrustManagersParameters();
        tsm.setKeyStore(tsp);

        sslContextParameters.setKeyManagers(kmp);
        sslContextParameters.setCertAlias("certificate");
        sslContextParameters.setTrustManagers(tsm);

        return sslContextParameters;
    }
}
