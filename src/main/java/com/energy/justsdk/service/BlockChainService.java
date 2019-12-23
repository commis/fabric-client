package com.energy.justsdk.service;

import java.util.Map;
import java.util.Set;
import org.hyperledger.fabric.sdk.Channel;

/**
 * 区块链维护对外提供接口：创建通道，节点加入通道，部署合约，合约实例化，合约升级等
 *
 * @author Bryan
 * @date 2019-12-06
 */
public interface BlockChainService {

    /**
     * 查询区块链上的Channel列表
     *
     * @return Channel列表
     */
    Set<String> queryAllChannels();

    /**
     * 在区块链上创建新通道
     *
     * @param channelName 通道名称
     * @return 返回通道信息
     */
    Channel createChannel(String channelName);

    /**
     * 将Peer加入到通道
     *
     * @param peerName 节点名称
     * @param channelName 通道名称
     */
    void peerJoinChannel(String peerName, String channelName);

    /**
     * 在区块链上安装智能合约
     *
     * @param ordererName orderer名称
     * @param peerName 节点名称
     * @param chaincodeName 智能合约名称
     * @param chaincodeType 智能合约类型：Java, Go
     * @param version 智能合约版本
     * @param chaincodePath 智能合约路径，相对路径
     */
    String installChaincode(String ordererName, String peerName,
        String chaincodeName, String chaincodeType, String version, String chaincodePath);

    /**
     * 实例化节点智能合约
     *
     * @param channelName 通道名称
     * @param ordererName orderer名称
     * @param peerName 节点名称
     * @param chaincodeName 智能合约名称
     * @param chaincodePath 智能合约路径，相对路径
     * @param version 智能合约版本
     * @param args 智能合约实例化参数
     * @param endorsePath endorse路径
     * @return 返回实例化结果
     */
    Map<String, String> instantieteChaincode(String channelName, String ordererName, String peerName,
        String chaincodeName, String chaincodePath, String version, String[] args, String endorsePath);

    /**
     * 升级节点智能合约
     *
     * @param channelName 通道名称
     * @param ordererName orderer名称
     * @param peerName 节点名称
     * @param chaincodeName 智能合约名称
     * @param chaincodePath 智能合约路径，相对路径
     * @param version 智能合约版本
     * @param args 智能合约参数
     * @param endorsePath endorse路径
     */
    Map<String, String> upgradeChaincode(String channelName, String ordererName, String peerName,
        String chaincodeName, String chaincodePath, String version, String[] args, String endorsePath);
}
