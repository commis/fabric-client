package com.energy.justsdk.model;

import lombok.Data;

/**
 * @author Bryan
 * @date 2019-12-06
 */
@Data
public class ChaincodeInfo {

    /** 智能合约名称 */
    private String chaincodeName;
    private String chaincodePeerName;
    /** 当前将要访问的智能合约所属频道名称 */
    private String chaincodeChannelName;
    /** 只能合约将要访问的排序名称*/
    private String chaincodeOrdererName;
    /** 合约安装路径: github.com/hyperledger/fabric/xxx/chaincode/go/example/test */
    private String chaincodePath;
    /** 智能合约版本号 */
    private String chaincodeVersion;
    /** 执行智能合约操作等待时间 */
//    private int invokeWatiTime = 100000;
    /** 执行智能合约实例等待时间 */
//    private int deployWatiTime = 120000;
}
