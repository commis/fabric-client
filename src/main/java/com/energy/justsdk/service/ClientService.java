package com.energy.justsdk.service;

import java.util.Map;

/**
 * 客户端对外接口，提供功能：合约查询，合约调用
 *
 * @author Bryan
 * @date 2019-12-06
 */
public interface ClientService {

    /**
     * 查询区块链上的区块数据
     *
     * @param blockSize 查询最大区块数量
     */
    Map<String, Object> queryBlock(int blockSize);

    /**
     * 查询区块链上的交易数据
     *
     * @param txId 交易ID
     */
    Map<String, Object> queryTransaction(String txId);

    /**
     * 调用合约查询数据
     *
     * @param fcn 合约函数名称
     * @param args 合约查询参数
     * @return 查询到的数据
     */
    Map<String, Object> query(String fcn, String[] args);

    /**
     * 调用合约接口执行合约功能
     *
     * @param fcn 合约函数名称
     * @param args 合约调用参数
     * @return 调用返回的数据
     */
    Map<String, Object> invoke(String fcn, String[] args);
}
