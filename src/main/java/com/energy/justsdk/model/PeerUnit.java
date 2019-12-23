package com.energy.justsdk.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Bryan
 * @date 2019-12-06
 */
@Data
@NoArgsConstructor
public class PeerUnit {

    private String peerName;
    private String peerDomainName;
    private String peerUrl;
}
