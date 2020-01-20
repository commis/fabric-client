package com.energy.justsdk.model;

import java.io.Serializable;
import java.util.Set;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;

/**
 * @author Bryan
 * @date 2019-12-06
 */
@Setter
@NoArgsConstructor
public class FabricUser implements User, Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String enrollSecret;
    private String mspid;
    private Enrollment enrollment;
    private Set<String> roles;
    private String account;
    private String affiliation;

    public FabricUser(String accessKey) {
        this.name = accessKey;
        this.mspid = accessKey + "MSP";
    }

    public String getEnrollSecret() {
        return enrollSecret;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getMspId() {
        return mspid;
    }

    @Override
    public Enrollment getEnrollment() {
        return enrollment;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public String getAccount() {
        return account;
    }

    @Override
    public String getAffiliation() {
        return affiliation;
    }
}
