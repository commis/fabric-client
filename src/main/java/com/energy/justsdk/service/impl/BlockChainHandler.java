package com.energy.justsdk.service.impl;

import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;

import com.energy.justsdk.service.config.NetworkProperty;
import com.energy.justsdk.model.OrdererUnit;
import com.energy.justsdk.model.PeerOrg;
import com.energy.justsdk.model.PeerUnit;
import com.energy.justsdk.model.UserContext;
import com.energy.justsdk.service.client.FabricClient;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.UpgradeProposalRequest;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private ClientHandler clientHandler;
    private NetworkProperty config;

    @PostConstruct
    void init() {
        this.config = clientHandler.getProperty();
    }

    /**
     * rootPath，channelTxPath 路径末尾必须带上目录分隔符
     */
    private void genChannelTx(String rootPath, String channelTxPath, String channelName)
        throws IOException, InterruptedException {
        File file;
        String[] args;
        String os = System.getProperty("os.name");
        if (os.startsWith("win") || os.startsWith("Win")) {
            file = new File(rootPath.replace("/", "\\"));
            args = new String[]{"cmd", "/c", "configtxgen",
                "-profile", config.getProfiles(),
                "-outputCreateChannelTx", channelTxPath.replace("/", "\\") + channelName + ".tx",
                "-channelID", channelName};
        } else {
            file = new File(rootPath);
            args = new String[]{rootPath + "/bin/configtxgen",
                "-profile", config.getProfiles(),
                "-outputCreateChannelTx", channelTxPath + channelName + ".tx",
                "-channelID", channelName};
        }

        Process process = Runtime.getRuntime().exec(args, null, file);
        //0: 代表正常退出
        if (process.waitFor() == 0) {
            log.info("Create {}.tx success", channelName);
        } else {
            log.info("Create {}.tx failed, exit code is {}", channelName, process.exitValue());
        }
        process.destroy();
    }

    public Set<String> queryAllChannels() throws Exception {
        Set<String> allChannel = new HashSet<>();
        try {
            /*HFClient client = handle.getClient();
            List<PeerOrg> peerOrgsList = handle.getConf().getPeerOrgsList();
            for (PeerOrg peerOrg : peerOrgsList) {
                String peerOrgDomainName = peerOrg.getPeers().get(0).getPeerDomainName();
                client.setUserContext(handle.getConf().enrollAdminUser(peerOrg.getOrgMspId(), peerOrgDomainName));
                allChannel.addAll(queryPeerChannel(peerOrg.getPeers(), client));
            }*/
            return allChannel;
        } catch (Exception e) {
            log.error("BlockChainHandler | QueryAllChannels | {}", e.getMessage());
            throw new Exception(e);
        }
    }

    /*private Set<String> queryPeerChannel(List<PeerUnit> peers, HFClient client) {
        Set<String> allChannel = new HashSet<>();
        for (PeerUnit peerUnit : peers) {
            try {
                Peer peer = client.newPeer(
                    peerUnit.getPeerName(),
                    peerUnit.getPeerUrl(),
                    handle.getConf().getPeerProperties(peerUnit.getPeerName()));
                Set<String> channels = client.queryChannels(peer);
                if (!ObjectUtils.isEmpty(channels)) {
                    allChannel.addAll(channels);
                }
            } catch (Exception e) {
                log.error("peer:{} has not yet joined the any channel. {}", peerUnit.getPeerName(), e.getMessage());
            }
        }
        return allChannel;
    }*/

    public Channel createChannel(String channelName) throws Exception {
        log.info("Running create new channel:{}", channelName);
        String path = config.getConfigPath() + "/conf";
        String channelTxPath = path + "/network/channel-artifacts/";
        genChannelTx(path, channelTxPath, channelName);

        File channelConfigurationFile = new File(channelTxPath + channelName + ".tx");
        if (!channelConfigurationFile.exists()) {
            throw new Exception(
                String.format("the channel config file %s is not exist.",
                    channelConfigurationFile.getName()));
        }

        PeerOrg adminPeerOrg = config.getChaincodeAdminPeerOrg(null);
        OrdererUnit ordererUnit = config.getChaincodeOrderUnit();
        UserContext orgAdmin = config.getEnrollAdminUser(adminPeerOrg);

        FabricClient fabClient = new FabricClient(orgAdmin);
        Orderer orderer = fabClient.getInstance().newOrderer(ordererUnit.getOrdererName(), ordererUnit.getOrdererUrl());
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(channelConfigurationFile);
        byte[] signature = fabClient.getInstance()
            .getChannelConfigurationSignature(channelConfiguration, orgAdmin);
        Channel newChannel = fabClient.getInstance().newChannel(channelName, orderer, channelConfiguration, signature);

        newChannel.addOrderer(orderer);
        newChannel.initialize();
        return newChannel;
    }

    public void peerJoinChannel(String peerName, String channelName) throws Exception {
        log.info("Running peer: {} join channel:{}", peerName, channelName);
        try {
            PeerOrg adminPeerOrg = config.getChaincodeAdminPeerOrg(null);
            UserContext orgAdmin = config.getEnrollAdminUser(adminPeerOrg);
            FabricClient fabClient = new FabricClient(orgAdmin);

            Channel channel = clientHandler.reconstructChannel(fabClient, channelName, peerName);
            Orderer orderer = channel.getOrderers().iterator().next();
            for (PeerOrg peerOrg : config.getPeerOrgsList()) {
                for (PeerUnit peerUnit : peerOrg.getPeers()) {
                    if (peerName.equals(peerUnit.getPeerName())) {
                        Peer peer = fabClient.getInstance().newPeer(
                            peerUnit.getPeerName(),
                            peerUnit.getPeerUrl(),
                            config.getPeerProperties(peerUnit.getPeerName()));
                        channel.joinPeer(orderer, peer, createPeerOptions());
                        break;
                    }
                }
            }

            if (!channel.isInitialized()) {
                try {
                    channel.initialize();
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("BlockChainHandler | peerJoinChannel | {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 安装智能合约接口
     *
     * @param ordererName orderer名称
     * @param peerName peer节点名称
     * @param chaincodeName 合约名称
     * @param chaincodeType 合约类型
     * @param version 合约版本
     * @param chaincodePath 合约相对路径，相对于fixture/chaincode/之后的相对路径
     * @return 合约安装的结果(成功或失败)
     * @throws Exception 出现错误的异常
     */
    public String installChaincode(String ordererName, String peerName, String chaincodeName, String chaincodeType,
        String version, String chaincodePath) throws Exception {
        log.info("Running install chaincode on {}", peerName);
        /*Collection<ProposalResponse> successful = new ArrayList<>();
        Collection<ProposalResponse> failed = new ArrayList<>();
        int numInstallProposal = 0;
        try {
            HFClient client = handle.getClient();
            String domainName = handle.getConf().getDomainName(peerName);
            client.setUserContext(handle.getConf().enrollAdminUser(ordererName, domainName));
            InstallProposalRequest installProposalRequest = getInstallProposalRequest(chaincodeName,
                chaincodeType, version, chaincodePath);
            Set<Peer> peerSet = new HashSet<>();
            for (PeerOrg peerOrg : handle.getConf().getPeerOrgsList()) {
                List<PeerUnit> peers = peerOrg.getPeers();
                for (PeerUnit peerUnit : peers) {
                    if (peerName.equals(peerUnit.getPeerName())) {
                        Peer peer = client.newPeer(peerUnit.getPeerName(), peerUnit.getPeerUrl(),
                            handle.getConf().getPeerProperties(peerUnit.getPeerName()));
                        peerSet.add(peer);
                    }
                }
            }
            Collection<ProposalResponse> proposalResponses = client
                .sendInstallProposal(installProposalRequest, peerSet);
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
                numInstallProposal, successful.size(), failed.size());
            return "Chaincode installed Successfully";
        } catch (Exception e) {
            log.error("BlockChainHandler | installChaincode |", e);
            throw new Exception("chaincode installtion failed", e);
        }*/
        return "";
    }

    private InstallProposalRequest getInstallProposalRequest(String chaincodeName, String chaincodeType,
        String version, String chaincodePath) throws InvalidArgumentException {
        /*ChaincodeID chaincodeID = ChaincodeID.newBuilder()
            .setName(chaincodeName)
            .setVersion(version)
            .setPath(chaincodePath)
            .build();
        log.info("Creating install proposal");
        InstallProposalRequest installProposalRequest = handle.getClient().newInstallProposalRequest();
        installProposalRequest.setChaincodeID(chaincodeID);
        String meta_inf_path = handle.getConf().getConfigPath() + "/chaincode/" + chaincodeID.getPath();
        boolean existMetaFile = isExistMetaFile(meta_inf_path);
        if (existMetaFile) {
            File metaFile = new File(meta_inf_path);
            if (metaFile.exists() && metaFile.isDirectory()) {
                //add indexes,if the directory exists the META-INF then add ,else no add
                installProposalRequest.setChaincodeMetaInfLocation(metaFile);
            }
        }
        installProposalRequest.setChaincodeVersion(chaincodeID.getVersion());
        String lowerChaincodeType = chaincodeType.toLowerCase();
        if (lowerChaincodeType.equals("go")) {
            installProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
            installProposalRequest.setChaincodeSourceLocation(Paths.get(handle.getConf().getConfigPath()).toFile());
        }
        if (lowerChaincodeType.equals("java")) {
            installProposalRequest.setChaincodeLanguage(TransactionRequest.Type.JAVA);
            String ccPath = handle.getConf().getConfigPath() + "/conf/chaincode/" + chaincodePath;
            installProposalRequest.setChaincodeSourceLocation(Paths.get(ccPath).toFile());
        }
        return installProposalRequest;*/
        return null;
    }

    private boolean isExistMetaFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            for (File everyFile : files) {
                if ("META-INF".equals(everyFile.getName())) {
                    flag = true;
                }
            }
        }
        return flag;
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
