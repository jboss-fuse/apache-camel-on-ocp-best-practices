package org.apache.camel.quarkus.httpssl;

import jakarta.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

@ApplicationScoped
public class HttpSslClientRouter extends EndpointRouteBuilder {

	@Override
	public void configure() throws Exception {
		from(platformHttp("/ping"))
				.to("direct:call-ssl-server");

		from("direct:call-ssl-server")
				.to("{{ssl-server.url}}?bridgeEndpoint=true&sslContextParameters=#clientConfig");
	}
}
