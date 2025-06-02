package org.xhy.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * API Premium Gateway 主启动类
 * 
 * @author xhy
 * @since 1.0.0
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
public class ApiPremiumGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiPremiumGatewayApplication.class, args);
    }
} 