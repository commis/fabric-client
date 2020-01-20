package com.energy.justsdk.service.client;

import com.energy.justsdk.model.FabricUser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Bryan
 * @date 2019-12-18
 */
@Slf4j
@Getter
public class CAClient {

    /**
     * Enroll admin user.
     *
     * @param username 用户名
     * @param password 用户密码
     */
    public FabricUser enrollUser(String username, String password) throws Exception {
        /*FabricUser fabricUser = UserContextUtil.readUserContext(adminContext.getAffiliation(), username);
        if (fabricUser != null) {
            log.warn("CA -{} admin is already enrolled.", caUrl);
            return fabricUser;
        }
        Enrollment adminEnrollment = instance.enroll(username, password);
        adminContext.setEnrollment(adminEnrollment);
        log.info("CA -{} enrolled admin.", caUrl);
        UserContextUtil.writeUserContext(adminContext);

        return adminContext;*/
        return null;
    }

}
