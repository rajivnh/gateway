package com.hcl.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Component
public class OAuthFilter extends AbstractGatewayFilterFactory<OAuthFilter.Config> {
	@Autowired
	private WebClient.Builder webClientBuilder;

	public OAuthFilter() {
		super(Config.class);
	}
	
	@Override
	public GatewayFilter apply(Config config) {
	    return ((exchange, chain) -> {	    	
	    	if(!exchange.getRequest().getPath().value().contains("/expired.html") && !exchange.getRequest().getPath().value().equals("/") && !exchange.getRequest().getPath().value().equals("/index.html"))
		    	if(exchange.getRequest().getPath().value().contains("/api/") || exchange.getRequest().getPath().value().endsWith(".html")) {
					String token = exchange.getRequest().getQueryParams().getFirst("token");
	
					if(token == null && exchange.getRequest().getHeaders().getFirst("X-AUTH-TOKEN") != null)
						token = exchange.getRequest().getHeaders().getFirst("X-AUTH-TOKEN").replaceAll("Bearer ", "");
				
					return webClientBuilder.baseUrl("http://OAUTH").build().post().uri("/oauth/check_token?token=" + token)
			        	.header("Content-Type", "application/json").accept(MediaType.APPLICATION_JSON)
			        	.retrieve()
			        	.onStatus((HttpStatus::isError), (it -> {
							return it.bodyToMono(String.class).flatMap(error -> {
								return Mono.error(new HttpClientErrorException(it.statusCode(), new String(error.getBytes())));
							});
						}))
			        	.bodyToMono(String.class).flatMap(s -> {
			        		return chain.filter(exchange);
			        	});
		    	}
		    	
		    	return chain.filter(exchange);
		    });
	}

	public static class Config {

	}
}