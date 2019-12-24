package com.energy.justsdk.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.energy.justsdk.model.OrdererUnit;
import com.energy.justsdk.model.PeerOrg;
import com.energy.justsdk.model.PeerUnit;
import com.energy.justsdk.service.client.ChannelClient;
import com.energy.justsdk.service.client.FabricClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.BlockchainInfo;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

/**
 * @author Bryan
 * @date 2019-12-06
 */
@Slf4j
@Getter
@Component
public class ClientHandler {

    @Autowired
    private ClientManager clientManager;

    public Map<String, Object> queryBlock(String channelName, String peerName, int blockSize) {
        try {
            ChannelClient channelClient = clientManager.getChannelClient(channelName, peerName);
            BlockchainInfo blockchainInfo = channelClient.getChannel().queryBlockchainInfo();

            NetworkBlockService blockService = new NetworkBlockService();
            JSONArray jsonArray = blockService.queryPeerBlock(blockchainInfo, channelClient.getChannel(), blockSize);
            return blockService.getResultJson(jsonArray);
        } catch (Exception e) {
            log.error("Querying block exception | {}", e.getMessage());
        }
        return null;
    }

    public Map<String, Object> queryTransaction(String channelName, String peerName, String txId) {
        Map<String, Object> map = new HashMap<>();
        try {
            ChannelClient channelClient = clientManager.getChannelClient(channelName, peerName);
            TransactionInfo txInfo = channelClient.queryByTransactionId(txId);

            map.put("txHash", txInfo.getTransactionID());
            map.put("validateCode", txInfo.getValidationCode().getNumber());
            map.put("payload", txInfo.getEnvelope().getPayload().toStringUtf8());
        } catch (Exception e) {
            log.error("Querying transaction exception | {}", e.getMessage());
        }
        return map;
    }

    public Map<String, Object> query(String channelName, String peerName, String chaincodename,
        String function, String[] chaincodeArgs) {
        Map<String, Object> callback = new HashMap<>();
        try {
            ChannelClient channelClient = clientManager.getChannelClient(channelName, peerName);
            log.debug("Querying chaincode {} and function {} with arguments {}",
                chaincodename, function, Arrays.asList(chaincodeArgs).toString());

            Collection<ProposalResponse> queryProposals = channelClient
                .queryByChainCode(chaincodename, function, chaincodeArgs);
            for (ProposalResponse proposalResponse : queryProposals) {
                if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                    String message = String.format(
                        "Failed query proposal from peer %s status: %s. Messages: %s. Was verified: %s",
                        proposalResponse.getPeer().getName(),
                        proposalResponse.getStatus(),
                        proposalResponse.getMessage(),
                        proposalResponse.isVerified());
                    log.error(message);
                    callback.put("state", 10010);
                    callback.put("payload", 0);
                    callback.put("message", message);
                    throw new Exception(message);
                } else {
                    String stringResponse = new String(proposalResponse.getChaincodeActionResponsePayload());
                    callback.put("state", 10000);
                    callback.put("payload", stringResponse);
                    callback.put("message", "success");
                }
            }
        } catch (Exception e) {
            callback.put("state", 10010);
            callback.put("message", e.getMessage());
            return callback;
        }
        return callback;
    }

    public Map<String, Object> invoke(String channelName, String peerName, String chaincodeName,
        String function, String[] chaincodeArgs) {
        Map<String, Object> callback = new HashMap<>();
        try {
            ChannelClient channelClient = clientManager.getChannelClient(channelName, peerName);
            log.debug("Invoking chaincode {} and function {} with arguments {}", chaincodeName,
                function, Arrays.asList(chaincodeArgs).toString());

            Collection<ProposalResponse> proposalResponses = channelClient
                .sendTransactionProposal(chaincodeName, function, chaincodeArgs);

            Collection<ProposalResponse> successful = new ArrayList<>();
            Collection<ProposalResponse> failed = new ArrayList<>();
            for (ProposalResponse proposalResponse : proposalResponses) {
                if (proposalResponse.isVerified() && proposalResponse.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(proposalResponse);
                } else {
                    failed.add(proposalResponse);
                }
            }

            if (failed.size() != 0) {
                ProposalResponse firstTxProposalResponse = failed.iterator().next();
                log.error("Not enough endorsers for inspect:{} endorser error: {}. Was verified: {}", failed.size(),
                    firstTxProposalResponse.getMessage(),
                    firstTxProposalResponse.isVerified());
                callback.put("state", 10010);
                callback.put("payload", firstTxProposalResponse.getMessage());
            } else {
                CompletableFuture<TransactionEvent> cf = channelClient.getChannel().sendTransaction(successful);
                ProposalResponse firstTxProposalResponse = successful.iterator().next();
                log.info("Successfully received transaction proposal responses.");
                callback.put("state", 10000);
                callback.put("message", "success");
                callback.put("payload", firstTxProposalResponse.getTransactionID());
            }
        } catch (Exception e) {
            callback.put("state", 10010);
            callback.put("message", e.getMessage());
            return callback;
        }
        log.info("Return result : {}", callback);
        return callback;
    }
}
