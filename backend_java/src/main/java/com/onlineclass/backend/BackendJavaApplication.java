package com.onlineclass.backend;

import lombok.extern.log4j.Log4j2;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Log4j2
@SpringBootApplication
@MapperScan("com.onlineclass.backend.mapper")
public class BackendJavaApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext application = SpringApplication.run(BackendJavaApplication.class, args);
        Environment env = application.getEnvironment();
        log.info("\n    ----------------------------------------------------------\n\t" +
                        "Local: \t\thttp://localhost:{}\n\t" +
                        "----------------------------------------------------------",
                env.getProperty("server.port"));
    }

}
