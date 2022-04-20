package com.luoxin;

import com.luoxin.config.GatewayRateLimitFilterByCpu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

  @Autowired private GatewayRateLimitFilterByCpu gatewayRateLimitFilterByCpu;

  @Bean
  public RouteLocator customerRouteLocator(RouteLocatorBuilder builder) {
    // return builder
    //     .routes()
    //     .route(r -> r.path("/aaa").filters(f -> f.filter(gatewayRateLimitFilterByCpu))
    //     .build();
    return builder
        .routes()
        .route(
            r ->
                r.path("/aaa")
                    .filters(f -> f.filter(gatewayRateLimitFilterByCpu))
                    .uri("http://localhost:8080/aaa"))
        .build();
  }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
