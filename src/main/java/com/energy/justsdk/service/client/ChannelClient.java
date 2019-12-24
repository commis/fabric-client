package com.energy.justsdk.service.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionInfo;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.springframework.util.StringUtils;

/**
 * @author Bryan
 * @date 2019-12-18
 */
@Slf4j
@Getter
@AllArgsConstructor
public class ChannelClient {

    private String name;
    private Channel channel;
    private FabricClient fabClient;

    /**
     * 查询智能合约数据
     *
     * @param chaincodeName 合约名称
     * @param functionName 函数名称
     * @param args 参数，格式：{"a", "b", "c"}
     */
    public Collection<ProposalResponse> queryByChainCode(String chaincodeName, String functionName, String[] args)
        throws Exception {
        log.info("Querying {} on channel {}", functionName, channel.getName());
        ChaincodeID ccid = ChaincodeID.newBuilder().setName(chaincodeName).build();
        QueryByChaincodeRequest request = fabClient.getInstance().newQueryProposalRequest();
        request.setChaincodeID(ccid);
        request.setFcn(functionName);
        if (args != null) {
            request.setArgs(args);
        }

        Map<String, byte[]> tm2 = new HashMap<>();
        tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
        tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
        tm2.put("result", ":)".getBytes(UTF_8));
        request.setTransientMap(tm2);

        Collection<ProposalResponse> response = channel.queryByChaincode(request);
        return response;
    }

    /**
     * 发送交易提议
     *
     * @param chaincodeName 合约名称
     * @param functionName 函数名称
     * @param args 参数，格式：{"a", "b", "c"}
     */
    public Collection<ProposalResponse> sendTransactionProposal(String chaincodeName, String functionName,
        String[] args) throws Exception {
        log.info("Sending transaction proposal on channel {}", channel.getName());
        TransactionProposalRequest request = fabClient.getInstance().newTransactionProposalRequest();
        request.setChaincodeID(ChaincodeID.newBuilder().setName(chaincodeName).build());
        request.setFcn(functionName);
        request.setArgs(args);
        request.setProposalWaitTime(180000);

        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes());
        tm.put("method", "TransactionProposalRequest".getBytes(UTF_8));
        tm.put("result", ":)".getBytes(UTF_8));
        request.setTransientMap(tm);

        Collection<ProposalResponse> response = channel.sendTransactionProposal(request, channel.getPeers());
        for (ProposalResponse pres : response) {
            String stringResponse = new String(pres.getChaincodeActionResponsePayload());
            log.info("Transaction proposal on channel {} {} with transaction id:{}, payload: {}",
                channel.getName(), pres.getStatus(), pres.getTransactionID(), stringResponse);
        }
        return response;
    }

    /**
     * 实例化智能合约
     *
     * @param chaincodeName 智能合约名称
     * @param chaincodePath 智能合约代码路径
     * @param version 智能合约版本
     * @param args 智能合约部署初始化参数
     * @param endorsePath 共识策略文件
     */
    public Collection<ProposalResponse> instantiateChainCode(String chaincodeName, String chaincodePath, String version,
        String[] args, String endorsePath) throws Exception {
        log.info("Instantiate proposal request {} on channel {} with Fabric client {} {}",
            chaincodeName, channel.getName(),
            fabClient.getInstance().getUserContext().getMspId(),
            fabClient.getInstance().getUserContext().getName());
        log.info("Instantiating Chaincode ID {} on channel {}", chaincodeName, channel.getName());

        ChaincodeID ccid = ChaincodeID.newBuilder()
            .setName(chaincodeName)
            .setPath(chaincodePath)
            .setVersion(version)
            .build();
        InstantiateProposalRequest request = fabClient.getInstance().newInstantiationProposalRequest();
        request.setChaincodeID(ccid);
        request.setArgs(args);

        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
        tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
        request.setTransientMap(tm);
        request.setProposalWaitTime(180000);

        if (!StringUtils.isEmpty(endorsePath)) {
            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromYamlFile(new File(endorsePath));
            request.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
        }

        Collection<ProposalResponse> responses = channel.sendInstantiationProposal(request);
        CompletableFuture<TransactionEvent> cf = channel.sendTransaction(responses);
        log.info("Chaincode {} on channel {} instantiation {}", chaincodeName, channel.getName(), cf);
        return responses;
    }

    /**
     * 根据交易ID查询交易信息
     *
     * @param txnId 交易ID
     */
    public TransactionInfo queryByTransactionId(String txnId) throws Exception {
        log.info("Querying by transaction id {} on channel {}", txnId, channel.getName());
        Peer peer = channel.getPeers().iterator().next();
        TransactionInfo info = channel.queryTransactionByID(peer, txnId);
        return info;
    }
}
