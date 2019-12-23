package com.energy.justsdk.service.client;

import com.energy.justsdk.model.UserContext;
import com.energy.justsdk.util.UserContextUtil;
import java.util.Properties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

/**
 * @author Bryan
 * @date 2019-12-18
 */
@Slf4j
@Getter
public class CAClient {

    String caUrl;
    Properties caProperties;
    HFCAClient instance;
    @Setter
    UserContext adminContext;

    public CAClient(String caUrl, Properties caProperties) throws Exception {
        this.caUrl = caUrl;
        this.caProperties = caProperties;
        this.instance = HFCAClient.createNewInstance(caUrl, caProperties);
        this.instance.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
    }

    /**
     * Enroll admin user.
     *
     * @param username 用户名
     * @param password 用户密码
     */
    public UserContext enrollAdminUser(String username, String password) throws Exception {
        UserContext userContext = UserContextUtil.readUserContext(adminContext.getAffiliation(), username);
        if (userContext != null) {
            log.warn("CA -{} admin is already enrolled.", caUrl);
            return userContext;
        }
        Enrollment adminEnrollment = instance.enroll(username, password);
        adminContext.setEnrollment(adminEnrollment);
        log.info("CA -{} enrolled admin.", caUrl);
        UserContextUtil.writeUserContext(adminContext);

        return adminContext;
    }

}
