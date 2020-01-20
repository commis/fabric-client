package com.energy.justsdk.service.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.alibaba.fastjson.JSONArray;
import com.energy.justsdk.model.FabricResult;
import com.energy.justsdk.model.FabricStateCode;
import com.energy.justsdk.model.FabricUser;
import com.energy.justsdk.service.impl.FabricBlockService;
import com.energy.justsdk.util.FileTools;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockchainInfo;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.NetworkConfig.OrgInfo;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.SDKUtils;
import org.hyperledger.fabric.sdk.TransactionInfo;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.helper.Config;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.yaml.snakeyaml.Yaml;

@Slf4j
public class FabricHelper {

    private JsonObject ymlConfJson;
    private String hashAlgo;
    private String accessKey;
    private String channelName;
    private String chaincodeName;
    private Map<String, HFClient> clientMap;
    private Map<String, Channel> channelMap;

    private FabricHelper() {
        channelMap = new HashMap<>();
        clientMap = new HashMap<>();
    }

    private static class Holder {

        static FabricHelper instance = new FabricHelper();
    }

    public static FabricHelper getInstance() {
        return Holder.instance;
    }

    @SuppressWarnings("unchecked")
    public void setConfigCtx(String configPath) {
        if (configPath == null || "".equals(configPath)) {
            log.error("config path is empty! please input correct ymal path!");
            throw new RuntimeException("config path is empty!");
        }
        try {
            Yaml yaml = new Yaml();
            InputStream stream = new FileInputStream(new File(configPath));
            Map<String, Object> confYaml = yaml.load(stream);
            //SDK内部NetworkConfig需要读取到私钥文件，这里如果配置的是目录，需要精确到文件。
            setAdminPrivateKeyFile((Map) confYaml.get("organizations"));

            this.ymlConfJson = Json.createObjectBuilder(confYaml).build();
            this.hashAlgo = ymlConfJson.getJsonObject("client").getJsonObject("BCCSP")
                .getJsonObject("security").getString("hashAlgorithm");
            this.accessKey = ymlConfJson.getJsonObject("client").getString("organization");

            JsonObject channels = ymlConfJson.getJsonObject("channels");
            //因为只有一个channel 所以只需获取键名即可
            String chanNameTemp = channels.keySet().toString();
            this.channelName = chanNameTemp.substring(chanNameTemp.indexOf("[") + 1, chanNameTemp.indexOf("]"));

            // 因为只有一个chaincode，所以只需取chaincode数组的第一个
            String codeNameTemp = channels.getJsonObject(channelName).getJsonArray("chaincodes").getString(0);
            this.chaincodeName = codeNameTemp.substring(0, codeNameTemp.indexOf(":"));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void setAdminPrivateKeyFile(Map<String, Object> organizations) {
        for (Map.Entry<String, Object> entry : organizations.entrySet()) {
            Object object = ((Map) entry.getValue()).get("adminPrivateKey");
            if (object == null) {
                continue;
            }
            try {
                File path = new File(((Map) object).get("path").toString());
                if (path.exists() && path.isDirectory()) {
                    File keyFile = FileTools.findFileSk(path);
                    ((Map) object).put("path", keyFile.getPath());
                }
            } catch (Exception e) {
                log.error("find admin private key file {}", e.getMessage());
            }
        }
    }

    private NetworkConfig loadFromYamlJson() {
        try {
            return NetworkConfig.fromJsonObject(ymlConfJson);
        } catch (Exception e) {
            log.error("can't load yaml configure Json.", e);
            return null;
        }
    }

    private Channel buildChannel(String channelName, NetworkConfig networkConfig, HFClient client) {
        try {
            FabricUser user = genFabricUser(accessKey);
            client.setUserContext(user);
            Channel channel = client.loadChannelFromConfig(channelName, networkConfig);
            channel.initialize();
            return channel;
        } catch (Exception e) {
            String msg = "can't construct channel: " + networkConfig.getClientOrganization();
            log.error(msg, e);
            return null;
        }
    }

    private HFClient getClient(String orgName) {
        HFClient client = clientMap.get(orgName);
        if (client == null) {
            synchronized (clientMap) {
                client = clientMap.get(orgName);
                if (client != null) {
                    return client;
                }

                client = HFClient.createNewInstance();
                try {
                    client.setCryptoSuite(getCryptoSuite());
                    clientMap.put(orgName, client);
                } catch (Exception e) {
                    String msg = "can't construct client: " + orgName;
                    log.error(msg, e);
                    return null;
                }
            }
        }
        return client;
    }

    private Channel getChannel(String accessKey) {
        HFClient client = getClient(accessKey);
        if (client == null) {
            return null;
        }
        return getChannel(accessKey, client);
    }

    private CryptoSuite getCryptoSuite() throws Exception {
        CryptoSuite cs;
        //for sm Algorithm
        if ("GMSM2".equals(this.hashAlgo)) {
            Properties properties = new Properties();
            properties.setProperty(Config.HASH_ALGORITHM, "SM3");
            properties.setProperty(Config.SIGNATURE_USERID, "1234567812345678");
            cs = CryptoSuite.Factory.getCryptoSuite(properties);
        } else {
            cs = CryptoSuite.Factory.getCryptoSuite();
        }
        return cs;
    }

    private Channel getChannel(String accessKey, HFClient client) {
        Channel channel = channelMap.get(accessKey);
        if (channel == null) {
            synchronized (channelMap) {
                channel = channelMap.get(accessKey);
                if (channel != null) {
                    return channel;
                }

                NetworkConfig networkConfig = loadFromYamlJson();
                if (networkConfig == null) {
                    return null;
                }

                try {
                    networkConfig.getOrdererNames().forEach(item -> {
                        try {
                            Properties p = networkConfig.getOrdererProperties(item);
                            p.setProperty("hostnameOverride", item);
                            p.setProperty("clientCertFile", getTlsCert("ordererorg"));
                            p.setProperty("clientKeyFile", getTlsKey("ordererorg"));
                            networkConfig.setOrdererProperties(item, p);
                        } catch (InvalidArgumentException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    networkConfig.getPeerNames().forEach(item -> {
                        try {
                            Properties p = networkConfig.getPeerProperties(item);
                            String orgId = getOrgIdByPeer(networkConfig, item);
                            p.setProperty("hostnameOverride", item);
                            p.setProperty("clientCertFile", getTlsCert(orgId));
                            p.setProperty("clientKeyFile", getTlsKey(orgId));
                            networkConfig.setPeerProperties(item, p);
                        } catch (InvalidArgumentException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    networkConfig.getEventHubNames().forEach(item -> {
                        try {
                            Properties p = networkConfig.getEventHubsProperties(item);
                            String orgId = getOrgIdByPeer(networkConfig, item);
                            p.setProperty("hostnameOverride", item);
                            p.setProperty("clientCertFile", getTlsCert(orgId));
                            p.setProperty("clientKeyFile", getTlsKey(orgId));
                            networkConfig.setEventHubProperties(item, p);
                        } catch (InvalidArgumentException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    networkConfig.getChannelNames().forEach(item -> {
                        if (channelName != "") {
                            channelName = item;
                        }
                    });

                } catch (Exception e) {
                    String msg = "can't get channel: " + accessKey;
                    log.error(msg, e);
                    return null;
                }

                channel = buildChannel(channelName, networkConfig, client);
                if (channel != null) {
                    channelMap.put(accessKey, channel);
                }
            }
        }

        return channel;
    }

    private JsonObject getJsonObjectOrg(String orgId) {
        JsonObject orgs = ymlConfJson.getJsonObject("organizations");
        for (Map.Entry o : orgs.entrySet()) {
            if (orgId.equals(o.getKey())) {
                JsonObject v = (JsonObject) o.getValue();
                return v;
            }
        }
        throw new RuntimeException(String.format("can't find orgId %s", orgId));
    }

    private String getTlsCert(String orgId) {
        JsonObject jsonOrg = getJsonObjectOrg(orgId);
        String ret = jsonOrg.getString("tlsCryptoCertPath");
        log.debug("tls cert for " + orgId + ",path:" + ret);
        return ret;
    }

    private String getTlsKey(String orgId) {
        JsonObject jsonOrg = getJsonObjectOrg(orgId);
        String ret = jsonOrg.getString("tlsCryptoKeyPath");
        log.debug("tls key for " + orgId + ",path:" + ret);
        return ret;
    }

    private String getOrgIdByPeer(NetworkConfig config, String peerName) {
        for (OrgInfo o : config.getOrganizationInfos()) {
            for (String p : o.getPeerNames()) {
                if (p.equals(peerName)) {
                    return o.getName();
                }
            }
        }
        return "";
    }

    private FabricUser genFabricUser(String accessKey) {
        FabricUser user = new FabricUser(accessKey);
        JsonObject jsonOrg = getJsonObjectOrg(accessKey);
        String adminPrivateKeyString = extractPemString(jsonOrg, "adminPrivateKey");
        String signedCert = extractPemString(jsonOrg, "signedCert");

        PrivateKey privateKey = null;
        try {
            privateKey = getPrivateKeyFromString(adminPrivateKeyString);
        } catch (Exception e) {
            log.error("Unable to parse private key: {}", e.getMessage());
            e.printStackTrace();
        }
        final PrivateKey finalPrivateKey = privateKey;
        user.setEnrollment(new Enrollment() {

            @Override
            public PrivateKey getKey() {
                return finalPrivateKey;
            }

            @Override
            public String getCert() {
                return signedCert;
            }
        });
        return user;
    }

    private String extractPemString(JsonObject jsonOrg, String key) {
        String pemString = "";
        JsonObject jsonObject = jsonOrg.getJsonObject(key);
        if (jsonObject != null) {
            String filePath = jsonObject.getString("path");
            try {
                File f = new File(filePath);
                if (f.exists() && !f.isDirectory()) {
                    FileInputStream stream = new FileInputStream(f);
                    pemString = IOUtils.toString(stream, UTF_8);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return pemString;
    }

    private PrivateKey getPrivateKeyFromString(String data) throws Exception {
        Reader pemReader = new StringReader(data);
        PEMParser pemParser = new PEMParser(pemReader);
        JcaPEMKeyConverter converter = (new JcaPEMKeyConverter());

        Object object = pemParser.readObject();
        return converter.getPrivateKey((PrivateKeyInfo) object);
    }

    /**
     * 发送交易提议
     *
     * @param method 函数名称
     * @param args 参数，格式：{"a", "b", "c"}
     */
    public FabricResult invokeByChainCode(String method, String[] args) {
        FabricResult fr = new FabricResult();
        HFClient client = getClient(accessKey);
        if (client == null) {
            return fr.failed(FabricStateCode.CLIENT_FAILED, "failed to get HFC client.");
        }

        Channel channel = getChannel(accessKey, client);
        if (channel == null) {
            return fr.failed(FabricStateCode.CHANNEL_FAILED, "failed to get channel.");
        }

        try {
            TransactionProposalRequest req = client.newTransactionProposalRequest();
            req.setChaincodeID(ChaincodeID.newBuilder().setName(chaincodeName).build());
            req.setFcn(method);
            req.setArgs(args);

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
            tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
            tm2.put("result", ":)".getBytes(UTF_8));
            req.setTransientMap(tm2);

            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            Collection<ProposalResponse> resps = channel.sendTransactionProposal(req);
            for (ProposalResponse response : resps) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }

            // Check that all the proposals are consistent with each other. We should have only one set
            // where all the proposals above are consistent.
            Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(resps);
            if (proposalConsistencySets.size() != 1) {
                String message = String.format("Expected only one set of consistent proposal responses but got %d: %s",
                    proposalConsistencySets.size(), Arrays.toString(args));
                log.error(message);
                return fr.failed(FabricStateCode.PROPOSAL_CONSISTENCY, message);
            }

            if (failed.size() > 0) {
                ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
                log.error("Not enough endorsers for {}: {}. endorser error: {}, Was verified: {}",
                    args, failed.size(),
                    firstTransactionProposalResponse.getMessage(),
                    firstTransactionProposalResponse.isVerified());
                return fr.failed(FabricStateCode.PROPOSAL_FAILED, firstTransactionProposalResponse.getMessage());
            }

            BlockEvent.TransactionEvent txEvent = channel.sendTransaction(successful).get(30, TimeUnit.SECONDS);
            if (txEvent.isValid()) {
                log.info("Finished transaction with transaction id {}: {}", txEvent.getTransactionID(), args);
                return fr.success(txEvent.getTransactionID());
            } else {
                String message = String.format("can't commit result: %s", Arrays.toString(args));
                log.error(message);
                return fr.failed(FabricStateCode.BLOCK_EVENT_FAILED, message);
            }
        } catch (Exception e) {
            log.error("invoke exception", e);
            return fr.failed(FabricStateCode.EXCEPTION, e.getMessage());
        }
    }

    /**
     * 查询智能合约数据
     *
     * @param method 函数名称
     * @param args 参数，格式：{"a", "b", "c"}
     */
    public FabricResult queryByChainCode(String method, String[] args) {
        FabricResult fr = new FabricResult();
        HFClient client = getClient(accessKey);
        if (client == null) {
            return fr.failed(FabricStateCode.CLIENT_FAILED, "failed to get HFC client.");
        }

        Channel channel = getChannel(accessKey, client);
        if (channel == null) {
            return fr.failed(FabricStateCode.CHANNEL_FAILED, "failed to get channel.");
        }

        try {
            QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
            queryByChaincodeRequest.setChaincodeID(ChaincodeID.newBuilder().setName(chaincodeName).build());
            queryByChaincodeRequest.setFcn(method);
            queryByChaincodeRequest.setArgs(args);

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
            tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
            tm2.put("result", ":)".getBytes(UTF_8));
            queryByChaincodeRequest.setTransientMap(tm2);

            Collection<ProposalResponse> proposalResponses = channel.queryByChaincode(queryByChaincodeRequest);
            for (ProposalResponse proposalResponse : proposalResponses) {
                if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                    String message = String.format(
                        "Failed query proposal status: %s. Messages: %s. Was verified: %s",
                        proposalResponse.getStatus(),
                        proposalResponse.getMessage(),
                        proposalResponse.isVerified());
                    log.error(message);
                    return fr.failed(FabricStateCode.PROPOSAL_FAILED, message);
                } else {
                    String payload = new String(proposalResponse.getChaincodeActionResponsePayload());
                    return fr.success(payload);
                }
            }
        } catch (Exception e) {
            log.error("query exception", e);
            return fr.failed(FabricStateCode.EXCEPTION, e.getMessage());
        }
        return fr.failed(FabricStateCode.PROPOSAL_FAILED, "query failed.");
    }

    /**
     * 根据交易ID查询交易信息
     *
     * @param txID 交易ID
     */
    public FabricResult queryTransactionByID(String txID) {
        FabricResult fr = new FabricResult();
        Channel channel = getChannel(accessKey);
        if (channel == null) {
            return fr.failed(FabricStateCode.CHANNEL_FAILED, "failed to get channel.");
        }

        try {
            TransactionInfo txInfo = channel.queryTransactionByID(txID);
            return fr.success(txInfo.toString());
        } catch (Exception e) {
            return fr.failed(FabricStateCode.EXCEPTION, "query transaction exception.");
        }
    }

    /**
     * 查询区块信息，区块层级有最大的限制
     *
     * @param blockSize 最大区块数量
     */
    public FabricResult queryBlock(int blockSize) {
        FabricResult fr = new FabricResult();
        Channel channel = getChannel(accessKey);
        if (channel == null) {
            return fr.failed(FabricStateCode.CHANNEL_FAILED, "failed to get channel.");
        }

        try {
            FabricBlockService blockService = new FabricBlockService();
            BlockchainInfo blockchainInfo = channel.queryBlockchainInfo();
            JSONArray jsonArray = blockService.queryPeerBlock(blockchainInfo, channel, blockSize);
            return fr.success(blockService.getResultJson(jsonArray).toString());
        } catch (Exception e) {
            String message = String.format("Querying block exception | {}", e.getMessage());
            log.error(message);
            return fr.failed(FabricStateCode.EXCEPTION, message);
        }
    }
}
