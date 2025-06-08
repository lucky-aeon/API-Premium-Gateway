package org.xhy.gateway;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * API Premium Gateway 主启动类
 * 
 * @author xhy
 * @since 1.0.0
 */
@SpringBootApplication(exclude = {
    HibernateJpaAutoConfiguration.class
})
@MapperScan("org.xhy.gateway.domain.**.repository")
public class ApiPremiumGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiPremiumGatewayApplication.class, args);
    }
} 