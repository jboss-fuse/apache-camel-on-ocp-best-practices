package org.apache.camel.quarkus.httpssl;

import jakarta.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

@ApplicationScoped
public class HttpSslServerRouter extends EndpointRouteBuilder {

	@Override
	public void configure() throws Exception {

		from(platformHttp("/ping"))
				.setBody().constant("pong");
	}

}
