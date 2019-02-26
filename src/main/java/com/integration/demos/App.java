package com.integration.demos;

import com.integration.demos.config.IntegrationConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

@SpringBootApplication
public class App {

    public static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {

        //Spring Batch Integration Solution
        SpringApplication.run(App.class, args);

    }

}
