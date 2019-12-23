package com.energy.justsdk.service.impl;

import com.energy.justsdk.service.BlockChainService;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Bryan
 * @date 2019-12-06
 */
@Slf4j
@Service
public class BlockChainServiceImpl implements BlockChainService {

    @Autowired
    private BlockChainHandler blockChainHandler;

    @Override
    public Set<String> queryAllChannels() {
        try {
            return blockChainHandler.queryAllChannels();
        } catch (Exception e) {
            log.error("query all channel exception: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Channel createChannel(String channelName) {
        try {
            return blockChainHandler.createChannel(channelName);
        } catch (Exception e) {
            log.error("create channel exception: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public void peerJoinChannel(String peerName, String channelName) {
        try {
            blockChainHandler.peerJoinChannel(peerName, channelName);
        } catch (Exception e) {
            log.error("peer join channel failed. {}", e.getMessage());
        }
    }

    @Override
    public String installChaincode(String ordererName, String peerName,
        String chaincodeName, String chaincodeType, String version, String chaincodePath) {
        try {
            return blockChainHandler.installChaincode(ordererName, peerName,
                chaincodeName, chaincodeType, version, chaincodePath);
        } catch (Exception e) {
            log.error("install chaincode: ", e);
        }
        return null;
    }

    @Override
    public Map<String, String> instantieteChaincode(String channelName, String ordererName, String peerName,
        String chaincodeName, String chaincodePath, String version, String[] args, String endorsePath) {
        try {
            return blockChainHandler.instantieteChaincode(channelName, ordererName, peerName,
                chaincodeName, chaincodePath, version, args, endorsePath);
        } catch (Exception e) {
            log.error("instantiete chaincode: ", e);
        }
        return null;
    }

    @Override
    public Map<String, String> upgradeChaincode(String channelName, String ordererName, String peerName,
        String chaincodeName, String chaincodePath, String version, String[] args, String endorsePath) {
        try {
            return blockChainHandler.upgradeChaincode(channelName, ordererName, peerName,
                chaincodeName, chaincodePath, version, args, endorsePath);
        } catch (Exception e) {
            log.error("upgrade chaincode: ", e);
        }
        return null;
    }
}
