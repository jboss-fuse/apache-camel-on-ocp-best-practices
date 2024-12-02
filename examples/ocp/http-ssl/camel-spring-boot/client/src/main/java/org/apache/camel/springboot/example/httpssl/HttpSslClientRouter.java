package org.apache.camel.springboot.example.httpssl;

import org.apache.camel.builder.RouteBuilder;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class HttpSslClientRouter extends RouteBuilder {
	@Override
	public void configure() throws Exception {
		rest()
				.get("ping")
				.produces(MediaType.TEXT_PLAIN_VALUE)
				.to("direct:call-ssl-server");

		from("direct:call-ssl-server")
				.to("{{ssl-server.url}}/ping?bridgeEndpoint=true&sslContextParameters=#clientConfig");
	}
}
