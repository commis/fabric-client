package com.energy.justsdk.model;

import java.io.Serializable;
import java.security.PrivateKey;
import lombok.AllArgsConstructor;
import org.hyperledger.fabric.sdk.Enrollment;

/**
 * @author Bryan
 * @date 2019-12-06
 */
@AllArgsConstructor
public class UserEnrollment implements Enrollment, Serializable {

    private static final long serialVersionUID = 1L;

    private PrivateKey key;
    private String certificate;

    @Override
    public PrivateKey getKey() {
        return key;
    }

    @Override
    public String getCert() {
        return certificate;
    }
}
