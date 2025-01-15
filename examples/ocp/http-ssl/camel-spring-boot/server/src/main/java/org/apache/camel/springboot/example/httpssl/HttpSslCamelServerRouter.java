package org.apache.camel.springboot.example.httpssl;

import org.apache.camel.builder.RouteBuilder;

import org.springframework.stereotype.Component;

@Component
public class HttpSslCamelServerRouter extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		from("undertow:{{exposed.server.url}}/ping?sslContextParameters=#serverConfig")
				.setBody().constant("pong");
	}
}
