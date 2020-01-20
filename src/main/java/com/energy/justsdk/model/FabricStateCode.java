package com.energy.justsdk.model;

/**
 * @author Bryan
 * @date 2020-01-14
 */
public class FabricStateCode {

    public static final Integer SUCCESS = 0;
    public static final Integer EXCEPTION = 10000;
    public static final Integer CLIENT_FAILED = 10001;
    public static final Integer CHANNEL_FAILED = 10002;

    public static final Integer PROPOSAL_CONSISTENCY = 10010;
    public static final Integer PROPOSAL_FAILED = 10011;
    public static final Integer BLOCK_EVENT_FAILED = 10012;
}
