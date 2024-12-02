package org.apache.camel.springboot.example.httpssl;

import org.apache.camel.CamelContext;
import org.apache.camel.support.jsse.ClientAuthentication;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.SSLContextServerParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SslConfiguration {

	private static final Logger log = LoggerFactory.getLogger(SslConfiguration.class);

	@Bean("serverConfig")
	public SSLContextParameters sslContextParameters(@Autowired CamelContext camelContext) {

		final String keyPassword = camelContext.resolvePropertyPlaceholders("{{secret:http-ssl-example-tls-password/password}}");
		final String keystoreFile = camelContext.resolvePropertyPlaceholders("{{secret-binary:ocp-ssl-camel-server-tls/keystore.jks}}");
		final String truststoreFile = camelContext.resolvePropertyPlaceholders("{{secret-binary:ocp-ssl-client-tls/truststore.jks}}");

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

		//server configuration
		final SSLContextServerParameters serverParam = new SSLContextServerParameters();
		serverParam.setClientAuthentication(ClientAuthentication.REQUIRE.name()); //two way ssl

		sslContextParameters.setKeyManagers(kmp);
		sslContextParameters.setCertAlias("certificate");
		sslContextParameters.setTrustManagers(tsm);
		sslContextParameters.setServerParameters(serverParam);

		return sslContextParameters;
	}
}
