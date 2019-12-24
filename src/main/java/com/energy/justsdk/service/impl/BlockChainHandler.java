package com.energy.justsdk.service.impl;

import com.energy.justsdk.model.PeerOrg;
import com.energy.justsdk.model.PeerUnit;
import com.energy.justsdk.model.UserContext;
import com.energy.justsdk.service.client.ChannelClient;
import com.energy.justsdk.service.client.FabricClient;
import com.energy.justsdk.service.config.NetworkProperty;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.UpgradeProposalRequest;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * 区块链的维护实现接口：创建通道，节点加入通道，部署合约，实例化合约，智能合约升级等功能
 *
 * @author Bryan
 * @date 2019-12-06
 */
@Slf4j
@Service
public class BlockChainHandler {

    @Autowired
    private ClientManager clientManager;

    /**
     * @param rootPath 网络的根路径
     */
    private void genChannelTx(String rootPath, String channelName)
        throws Exception {
        String networkPath = rootPath + "/network";
        String configTxGenBin = rootPath + "/bin/configtxgen";
        String channelTxFile = String.format("%s/channel-artifacts/%s.tx", networkPath, channelName);

        Process process;
        String os = System.getProperty("os.name");
        if (os.startsWith("win") || os.startsWith("Win")) {
            File dir = new File(networkPath.replace("/", "\\"));
            String[] args = {"cmd", "/c", configTxGenBin.replace("/", "\\"),
                "-profile", clientManager.getProperty().getProfile(),
                "-outputCreateChannelTx", channelTxFile.replace("/", "\\"),
                "-channelID", channelName};
            log.info("execute: {}", String.join(" ", args));
            process = Runtime.getRuntime().exec(args, null, dir);
        } else {
            File dir = new File(networkPath);
            String[] args = {configTxGenBin,
                "-profile", clientManager.getProperty().getProfile(),
                "-outputCreateChannelTx", channelTxFile,
                "-channelID", channelName};
            log.info("execute: {}", String.join(" ", args));
            process = Runtime.getRuntime().exec(args, null, dir);
        }

        //0: 代表正常退出
        if (process.waitFor() == 0) {
            log.info("Create {}.tx success", channelName);
        } else {
            log.info("Create {}.tx failed, exit code is {}", channelName, process.exitValue());
        }
        process.destroy();

        File channelTx = new File(channelTxFile);
        if (!channelTx.exists()) {
            throw new Exception(String.format("the channel config file %s is not exist.", channelTxFile));
        }
    }

    public Set<String> queryAllChannels() throws Exception {
        Set<String> allChannel = new HashSet<>();
        try {
            HFClient client = HFClient.createNewInstance();
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            List<PeerOrg> peerOrgsList = clientManager.getProperty().getPeerOrgsList();
            for (PeerOrg peerOrg : peerOrgsList) {
                client.setUserContext(clientManager.getProperty().getEnrollAdminUser(peerOrg));
                allChannel.addAll(queryPeerChannel(peerOrg.getPeers(), client));
            }
            return allChannel;
        } catch (Exception e) {
            log.error("BlockChainHandler | QueryAllChannels | {}", e.getMessage());
            throw new Exception(e);
        }
    }

    private Set<String> queryPeerChannel(List<PeerUnit> peers, HFClient client) {
        Set<String> allChannel = new HashSet<>();
        for (PeerUnit peerUnit : peers) {
            try {
                Peer peer = client.newPeer(
                    peerUnit.getPeerName(),
                    peerUnit.getPeerUrl(),
                    clientManager.getProperty().getPeerProperties(peerUnit.getPeerName()));
                Set<String> channels = client.queryChannels(peer);
                if (!ObjectUtils.isEmpty(channels)) {
                    allChannel.addAll(channels);
                }
            } catch (Exception e) {
                log.error("peer:{} has not yet joined the any channel. {}", peerUnit.getPeerName(), e.getMessage());
            }
        }
        return allChannel;
    }

    public String createChannel(String channelName) throws Exception {
        log.info("Running create new channel:{}", channelName);
        if (clientManager.isExist(channelName)) {
            throw new Exception(String.format("The channel %s is exist.", channelName));
        }

        NetworkProperty property = clientManager.getProperty();
        genChannelTx(property.getNetworkPath(), channelName);

        String chaincodePeerName = property.getChaincodeInfo().getChaincodePeerName();
        PeerOrg adminPeerOrg = property.getChaincodeAdminPeerOrg(chaincodePeerName);
        UserContext orgAdmin = property.getEnrollAdminUser(adminPeerOrg);

        FabricClient fabClient = new FabricClient(orgAdmin);
        Channel newChannel = fabClient.createChannel(property, channelName);
        for (PeerUnit peer : adminPeerOrg.getPeers()) {
            if (peer.getPeerName().equals(chaincodePeerName)) {
                newChannel.addPeer(fabClient.getInstance().newPeer(
                    peer.getPeerName(),
                    peer.getPeerUrl(),
                    property.getPeerProperties(peer.getPeerName())));
            }
        }
        clientManager.addChannelClient(fabClient, channelName, newChannel);

        return newChannel.toString();
    }

    public void peerJoinChannel(String peerName, String channelName) throws Exception {
        log.info("Running peer: {} join channel:{}", peerName, channelName);
        if (!clientManager.isExist(channelName)) {
            throw new Exception(String.format("The channel %s is not exist.", channelName));
        }

        ChannelClient channelClient = clientManager.getChannelClient(channelName, peerName);
        NetworkProperty property = clientManager.getProperty();
        PeerUnit peerUnit = property.getChaincodePeer(peerName);
        channelClient.getChannel().addPeer(
            channelClient.getFabClient().getInstance().newPeer(
                peerUnit.getPeerName(),
                peerUnit.getPeerUrl(),
                property.getPeerProperties(peerUnit.getPeerName())));

        channelClient.getChannel().initialize();
    }

    /**
     * 安装智能合约接口
     *
     * @param peerName peer节点名称
     * @param chaincodeName 合约名称
     * @param version 合约版本
     * @param chaincodePath 合约相对路径，相对于/chaincode/之后的相对路径
     * @return 合约安装的结果(成功或失败)
     * @throws Exception 出现错误的异常
     */
    public String installChaincode(String peerName, String chaincodeName, String version, String chaincodePath)
        throws Exception {
        log.info("Running install chaincode on {}", peerName);
        NetworkProperty property = clientManager.getProperty();
        ChannelClient channelClient = clientManager.getChannelClient(
            property.getChaincodeInfo().getChaincodeChannelName(), peerName);

        PeerUnit peerUnit = property.getChaincodePeer(peerName);
        List<Peer> peers = Arrays.asList(
            channelClient.getFabClient().getInstance().newPeer(
                peerUnit.getPeerName(),
                peerUnit.getPeerUrl(),
                property.getPeerProperties(peerUnit.getPeerName()))
        );

        Collection<ProposalResponse> proposalResponses = channelClient.getFabClient()
            .deployChainCode(chaincodeName, chaincodePath, version, peers);

        Collection<ProposalResponse> successful = new ArrayList<>();
        Collection<ProposalResponse> failed = new ArrayList<>();
        for (ProposalResponse pr : proposalResponses) {
            if (pr.getStatus() == ProposalResponse.Status.SUCCESS) {
                log.debug("Successful install proposal response Txid: {} from peer {}",
                    pr.getTransactionID(), pr.getPeer().getName());
                successful.add(pr);
            } else {
                failed.add(pr);
            }
        }
        if (failed.size() > 0) {
            ProposalResponse next = failed.iterator().next();
            String errMsg = "Not enough endorsers for install:" + successful.size() + ". " + next.getMessage();
            throw new Exception(errMsg);
        }
        log.info("Received {} install proposal response.Successful+Verified: {}.Failed: {}",
            peers.size(), successful.size(), failed.size());
        return "Chaincode installed Successfully";
    }

    public Map<String, String> instantieteChaincode(String channelName, String ordererName, String peerName,
        String chaincodeName, String chaincodePath, String version, String[] args, String endorsePath)
        throws Exception {
        /*Collection<ProposalResponse> successful = new ArrayList<>();
        Collection<ProposalResponse> failed = new ArrayList<>();
        Map<String, String> callback = new HashMap<>();
        log.info("Running instantiate chaincode on chanel:{}", channelName);
        try {
            Channel channel = handle.reconstructChannel(channelName, ordererName, peerName);
            HFClient client = handle.getClient();
            String domainName = handle.getConf().getDomainName(peerName);
            client.setUserContext(handle.getConf().enrollAdminUser(ordererName, domainName));
            InstantiateProposalRequest instantiateProposalRequest = getInstantiateProposalRequest(chaincodeName,
                chaincodePath, version, args, endorsePath);
            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(StandardCharsets.UTF_8));
            tm.put("method", "InstantiateProposalRequest".getBytes(StandardCharsets.UTF_8));
            tm.put("result", ":)".getBytes(StandardCharsets.UTF_8));
            instantiateProposalRequest.setTransientMap(tm);
            Collection<ProposalResponse> proposalResponses = channel
                .sendInstantiationProposal(instantiateProposalRequest);
            for (ProposalResponse response : proposalResponses) {
                if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                    callback.put("status", "success");
                    callback.put("data", "");
                    channel.sendTransaction(successful).get(100000, TimeUnit.SECONDS);
                    log.info("Succesful instantiate proposal response Txid: {} from peer {}",
                        response.getTransactionID(), response.getPeer().getName());
                } else {
                    failed.add(response);
                    ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
                    callback.put("status", "error");
                    callback.put("data", firstTransactionProposalResponse.getMessage());
                    log.info("Instantiate chaincode failed");
                }
            }
            log.info("Received {} instantiate proposal responses. Successful+verified: {} . Failed: {}",
                proposalResponses.size(), successful.size(), failed.size());
            if (failed.size() > 0) {
                ProposalResponse first = failed.iterator().next();
                String errMsg = "Chaincode instantiation failed , reason " + "Not enough endorsers for instantiate :"
                    + successful.size() + "endorser failed with " + first.getMessage() + ". Was verified:"
                    + first.isVerified();
                throw new Exception(errMsg);
            }
            log.info("Sending instantiateTransaction to orderer");
            return callback;
        } catch (Exception e) {
            String errMsg = "BlockChainHandler | instantiateChaincode |" + e;
            log.error(errMsg);
            throw new Exception(errMsg, e);
        }*/
        return null;
    }

    private InstantiateProposalRequest getInstantiateProposalRequest(String chaincodeName, String chaincodePath,
        String version, String[] chaincodeArgs, String endorsePath)
        throws IOException, ChaincodeEndorsementPolicyParseException {
        /*ChaincodeID chaincodeID = ChaincodeID.newBuilder()
            .setName(chaincodeName)
            .setPath(chaincodePath)
            .setVersion(version)
            .build();
        InstantiateProposalRequest instantiateProposalRequest = handle.getClient()
            .newInstantiationProposalRequest();
        instantiateProposalRequest.setChaincodeID(chaincodeID);
        instantiateProposalRequest.setArgs(chaincodeArgs);
        instantiateProposalRequest.setProposalWaitTime(100000);
        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(new File(endorsePath));
        instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
        return instantiateProposalRequest;*/
        return null;
    }

    public Map<String, String> upgradeChaincode(String channelName, String ordererName, String peerName,
        String chaincodeName, String chaincodePath, String version, String[] args, String endorsePath)
        throws Exception {
        /*Collection<ProposalResponse> successful = new ArrayList<>();
        Collection<ProposalResponse> failed = new ArrayList<>();
        Map<String, String> callback = new HashMap<>();
        log.info("Running upgrade chaincode on channel:{}", channelName);
        try {
            Channel channel = handle.reconstructChannel(channelName, ordererName, peerName);
            HFClient client = handle.getClient();
            String domainName = handle.getConf().getDomainName(peerName);
            client.setUserContext(handle.getConf().enrollAdminUser(ordererName, domainName));
            UpgradeProposalRequest upgradeProposalRequest = getUpgradeProposalRequest(chaincodeName,
                chaincodePath, version, args, endorsePath);
            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "UpgradeProposalRequest:JavaSDK".getBytes(StandardCharsets.UTF_8));
            tm.put("method", "UpgradeProposalRequest".getBytes(StandardCharsets.UTF_8));
            tm.put("result", ":)".getBytes(StandardCharsets.UTF_8));
            upgradeProposalRequest.setTransientMap(tm);
            Collection<ProposalResponse> proposalResponses = channel.sendUpgradeProposal(upgradeProposalRequest);
            for (ProposalResponse response : proposalResponses) {
                if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                    ProposalResponse resp = proposalResponses.iterator().next();
                    callback.put("status", "success");
                    callback.put("data", "");
                    channel.sendTransaction(successful).get(100000, TimeUnit.SECONDS);
                    log.info("Succesful upgrade proposal response Txid: {} from peer {}",
                        response.getTransactionID(), response.getPeer().getName());
                } else {
                    failed.add(response);
                    ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
                    callback.put("status", "error");
                    callback.put("data", firstTransactionProposalResponse.getMessage());
                    log.info("Upgrade chaincode failed");
                }
            }
            log.info("Received {} upgrade proposal responses. Successful+verified: {}. Failed: {}",
                proposalResponses.size(), successful.size(), failed.size());
            if (failed.size() > 0) {
                ProposalResponse first = failed.iterator().next();
                String errMsg = "Chaincode Upgrade failed , reason " + "Not enough endorsers for upgrade :"
                    + successful.size() + "endorser failed with " + first.getMessage() + ". Was verified:"
                    + first.isVerified();
                throw new Exception(errMsg);
            }
            log.info("Sending upgradeTransaction to orderer");
            return callback;
        } catch (Exception e) {
            String errMsg = "BlockChainHandler | Upgrade chaincode |" + e;
            log.error(errMsg);
            throw new Exception(errMsg, e);
        }*/
        return null;
    }

    private UpgradeProposalRequest getUpgradeProposalRequest(String chaincodeName, String chaincodePath,
        String version, String[] chaincodeArgs, String endorsePath)
        throws IOException, ChaincodeEndorsementPolicyParseException {
        /*ChaincodeID chaincodeID = ChaincodeID.newBuilder()
            .setName(chaincodeName)
            .setPath(chaincodePath)
            .setVersion(version)
            .build();
        UpgradeProposalRequest upgradeProposalRequest = handle.getClient().newUpgradeProposalRequest();
        upgradeProposalRequest.setChaincodeID(chaincodeID);
        upgradeProposalRequest.setArgs(chaincodeArgs);
        upgradeProposalRequest.setProposalWaitTime(100000);
        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(new File(endorsePath));
        upgradeProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
        return upgradeProposalRequest;*/
        return null;
    }

}
