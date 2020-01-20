package com.energy.justsdk.service.impl;

import com.energy.justsdk.service.client.FabricHelper;
import java.io.File;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.helper.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author Bryan
 * @date 2020-01-14
 */
@Slf4j
@Service
public class ClientManager {

    @Value("${server.network}")
    private String sdkConfigFile;

    @PostConstruct
    void afterConstruct() {
        String sdkPath = Objects.requireNonNull(
            this.getClass().getClassLoader().getResource("fixture")).getFile();
        if (!StringUtils.isEmpty(sdkPath)) {
            sdkConfigFile = sdkPath + File.separator + "fabric-sdk-config.yaml";
        }
        String config = sdkPath + File.separator + "config.properties";
        if ((new File(config)).exists()) {
            System.setProperty(Config.ORG_HYPERLEDGER_FABRIC_SDK_CONFIGURATION, config);
        }

        log.info("sdk config: {}", sdkConfigFile);
        if (new File(sdkConfigFile).exists()) {
            FabricHelper.getInstance().setConfigCtx(sdkConfigFile);
        } else {
            throw new RuntimeException(String.format("Can't find sdk config in path %s", sdkConfigFile));
        }
    }
}
