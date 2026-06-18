package app.keystone.integrationTest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 集成测试配置类
 * @author likanug
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class IntegrationTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationTestApplication.class, args);
    }
}
