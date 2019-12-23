package com.energy.justsdk.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Bryan
 * @date 2019-12-06
 */
@Data
@NoArgsConstructor
public class PeerOrg {

    private String orgName;
    private String orgMspId;
    private List<PeerUnit> peers = new ArrayList<>();

    public void addPeerUnit(PeerUnit peerUnit) {
        if (peerUnit != null) {
            this.peers.add(peerUnit);
        }
    }
}
