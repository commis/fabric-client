package com.energy.justsdk.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Bryan
 * @date 2019-12-06
 */
@Data
@NoArgsConstructor
public class OrdererUnit {

    private String mspid;
    private String domainName;
    private String ordererName;
    private String ordererUrl;
}
