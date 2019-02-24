package com.integration.demos.utils;

import com.integration.demos.App;
import com.integration.demos.config.IntegrationConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

public class AppUtils {

    public static final Logger log = LoggerFactory.getLogger(AppUtils.class);

    public static void setupData() {
        try {
            log.info("Copying sample file to be processed in dir: " + IntegrationConfig.INPUT_DIRECTORY);
            Resource resource = new ClassPathResource("data/benefitiaries-info.txt");
            FileUtils.copyFileToDirectory(resource.getFile(), new File(IntegrationConfig.INPUT_DIRECTORY));
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
