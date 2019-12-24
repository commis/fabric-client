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

    private String domainName;
    private String peerName;
    private String peerUrl;
}
