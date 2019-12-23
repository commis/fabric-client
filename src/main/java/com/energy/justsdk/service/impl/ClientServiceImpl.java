package com.energy.justsdk.service.impl;

import com.energy.justsdk.model.ChaincodeInfo;
import com.energy.justsdk.service.ClientService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Bryan
 * @date 2019-12-06
 */
@Slf4j
@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    private ClientHandler handle;

    @Override
    public Map<String, Object> queryBlock(int blockSize) {
        ChaincodeInfo chaincodeInfo = handle.getProperty().getChaincodeInfo();
        String channelName = chaincodeInfo.getChaincodeChannelName();
        String peerName = chaincodeInfo.getChaincodePeerName();
        return handle.queryBlock(channelName, peerName, blockSize);
    }

    @Override
    public Map<String, Object> queryTransaction(String txId) {
        ChaincodeInfo chaincodeInfo = handle.getProperty().getChaincodeInfo();
        String channelName = chaincodeInfo.getChaincodeChannelName();
        String peerName = chaincodeInfo.getChaincodePeerName();
        return handle.queryTransaction(channelName, peerName, txId);
    }

    @Override
    public Map<String, Object> query(String fcn, String[] args) {
        ChaincodeInfo chaincodeInfo = handle.getProperty().getChaincodeInfo();
        String channelName = chaincodeInfo.getChaincodeChannelName();
        String peerName = chaincodeInfo.getChaincodePeerName();
        String chaincodeName = chaincodeInfo.getChaincodeName();
        return handle.query(channelName, peerName, chaincodeName, fcn, args);
    }

    @Override
    public Map<String, Object> invoke(String fcn, String[] args) {
        ChaincodeInfo chaincodeInfo = handle.getProperty().getChaincodeInfo();
        String channelName = chaincodeInfo.getChaincodeChannelName();
        String peerName = chaincodeInfo.getChaincodePeerName();
        String chaincodeName = chaincodeInfo.getChaincodeName();
        return handle.invoke(channelName, peerName, chaincodeName, fcn, args);
    }
}
