package com.energy.justsdk.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Bryan
 * @date 2019-12-06
 */
@Data
@NoArgsConstructor
public class BlockInfoBean {

    private int blockHeight;
    private int txCount;
    private String txHash;
    private String mspid;
    private String channel;
    private String time;
    private String diffTime;
}
