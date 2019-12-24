package com.energy.justsdk.service.config;

import com.energy.justsdk.model.ChaincodeInfo;
import com.energy.justsdk.model.NetworkNodeEnum;
import com.energy.justsdk.model.OrdererUnit;
import com.energy.justsdk.model.PeerOrg;
import com.energy.justsdk.model.PeerUnit;
import com.energy.justsdk.model.UserContext;
import com.energy.justsdk.util.UserContextUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.hyperledger.fabric.sdk.Enrollment;
import org.springframework.util.StringUtils;

/**
 * @author Bryan
 * @date 2019-12-06
 */
@Getter
public class NetworkProperty {

    private boolean isTls;
    private String networkPath;
    private String profile;
    private List<PeerUnit> joinedPeers = new ArrayList<>();
    private ChaincodeInfo chaincodeInfo = new ChaincodeInfo();
    private List<OrdererUnit> ordererList = new ArrayList<>();
    private List<PeerOrg> peerOrgsList = new ArrayList<>();

    public NetworkProperty(String configPath, String networkPath) throws IOException {
        this.networkPath = networkPath;
        Properties properties = new Properties();
        properties.load(new FileInputStream(configPath + "/sdk.properties"));
        this.isTls = "true".equals(getPropertyValue(properties, Constant.ISTLS));
        this.profile = getPropertyValue(properties, Constant.PROFILES);
        this.chaincodeInfo.setChaincodeName(getPropertyValue(properties, Constant.CHAINCODENAME));
        this.chaincodeInfo.setChaincodePeerName(getPropertyValue(properties, Constant.CHAINCODEPEERNAME));
        this.chaincodeInfo.setChaincodeChannelName(getPropertyValue(properties, Constant.CHAINCODECHANNELNAME));
        this.chaincodeInfo.setChaincodeOrdererName(getPropertyValue(properties, Constant.CHAINCODEORDERERNAME));
        this.chaincodeInfo.setChaincodePath(getPropertyValue(properties, Constant.CHAINCODEPATH));
        this.chaincodeInfo.setChaincodeVersion(getPropertyValue(properties, Constant.CHAINCODEVERSION));
        parseOrderers(properties);
        parseOrgs(properties);
        parseJoinedPeers(properties);
    }

    private String getPropertyValue(Properties properties, String key) {
        String value = properties.getProperty(key);
        return (value != null) ? value : "";
    }

    /**
     * @param str 格式：domainname=example.com,orderername=org1.example.com,ordererurl=grpc://x.x.x.1:7050
     */
    private OrdererUnit parseOrderer(String str) {
        if (!StringUtils.isEmpty(str)) {
            OrdererUnit orderer = new OrdererUnit();
            String[] split = str.split(",");
            for (String s : split) {
                String[] kv = s.split("=");
                if ("mspid".equals(kv[0])) {
                    orderer.setMspid(kv[1]);
                }
                if ("domainname".equals(kv[0])) {
                    orderer.setDomainName(kv[1]);
                }
                if ("orderername".equals(kv[0])) {
                    orderer.setOrdererName(kv[1]);
                }
                if ("ordererurl".equals(kv[0])) {
                    if (isTls()) {
                        kv[1] = kv[1].replaceFirst("^grpc://", "grpcs://");
                    }
                    orderer.setOrdererUrl(kv[1]);

                }
            }
            return orderer;
        }
        return null;
    }

    /**
     * @param str 格式：orgname=org1,mspid=org1MSP
     */
    private PeerOrg parseOrg(String str) {
        if (!StringUtils.isEmpty(str)) {
            PeerOrg peerOrg = new PeerOrg();
            String[] split = str.split(",");
            for (String s : split) {
                String[] kv = s.split("=");
                if ("orgname".equals(kv[0])) {
                    peerOrg.setOrgName(kv[1]);
                }
                if ("mspid".equals(kv[0])) {
                    peerOrg.setOrgMspId(kv[1]);
                }
            }
            return peerOrg;
        }
        return null;
    }

    /**
     * @param str 格式：domainname=org1,peername=peer0.org1,peerurl=grpc://x.x.x.1:7051
     */
    private PeerUnit parsePeer(String str) {
        if (!StringUtils.isEmpty(str)) {
            PeerUnit peer = new PeerUnit();
            String[] split = str.split(",");
            for (String s : split) {
                String[] kv = s.split("=");
                if ("domainname".equals(kv[0])) {
                    peer.setDomainName(kv[1]);
                }
                if ("peername".equals(kv[0])) {
                    peer.setPeerName(kv[1]);
                }
                if ("peerurl".equals(kv[0])) {
                    if (isTls()) {
                        kv[1] = kv[1].replaceFirst("^grpc://", "grpcs://");
                    }
                    peer.setPeerUrl(kv[1]);
                }
            }
            return peer;
        }
        return null;
    }

    private void parseOrgs(Properties properties) {
        //解析org的定义，格式：org=org1,org2,org3,org4
        String orgs = getPropertyValue(properties, Constant.ORG);
        if (!StringUtils.isEmpty(orgs)) {
            String[] orgList = orgs.split(",");
            for (String s : orgList) {
                String property = getPropertyValue(properties, s);
                if (!StringUtils.isEmpty(property)) {
                    String[] peerSplit = property.split(",");
                    //每个Org的第一个Peer节点作为锚点
                    PeerOrg peerOrg = parseOrg(getPropertyValue(properties, s + peerSplit[0]));
                    if (peerOrg != null) {
                        for (String ps : peerSplit) {
                            String key = s + ps;
                            String property1 = getPropertyValue(properties, key);
                            PeerUnit peer = parsePeer(property1);
                            peerOrg.addPeerUnit(peer);
                        }
                        this.peerOrgsList.add(peerOrg);
                    }
                }
            }
        }
    }

    private void parseOrderers(Properties properties) {
        //解析orderer的定义，格式：orderers=orderer1,orderer2,orderer3,orderer4
        String orderers = getPropertyValue(properties, Constant.ORDERERS);
        if (!StringUtils.isEmpty(orderers)) {
            String[] ordererSplit = orderers.split(",");
            for (String s : ordererSplit) {
                String property = getPropertyValue(properties, s);
                OrdererUnit orderer = parseOrderer(property);
                if (orderer != null) {
                    this.ordererList.add(orderer);
                }
            }
        }
    }

    private void parseJoinedPeers(Properties properties) {
        String joinedPeers = getPropertyValue(properties, Constant.JOINEDPEER);
        Set<String> peers = new HashSet<>(Arrays.asList(joinedPeers.split(",")));
        for (PeerOrg peerOrg : peerOrgsList) {
            for (PeerUnit peer : peerOrg.getPeers()) {
                if (peers.contains(peer.getPeerName())) {
                    this.joinedPeers.add(peer);
                }
            }
        }
    }

    /**
     * 从全名中获取名称，根据网络实际配置情况进行修改
     */
    public String getName(final String name) {
        int dot = name.indexOf(".");
        return (-1 == dot) ? "" : name.substring(0, dot);
    }

    /**
     * 从全名中获取域名，根据网络实际配置情况进行修改
     */
    public String getDomainName(final String name) {
        int dot = name.indexOf(".");
        return (-1 == dot) ? "" : name.substring(dot + 1);
    }

    private File findFileSk(File directory) {
        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));
        if (null == matches) {
            throw new RuntimeException(
                String.format("Matches returned null does %s directory exist?",
                    directory.getAbsoluteFile().getName()));
        }
        if (matches.length != 1) {
            throw new RuntimeException(
                String.format("Expected in {} only 1 sk file but found {}",
                    directory.getAbsoluteFile().getName(), matches.length));
        }
        return matches[0];
    }

    public Properties getOrdererProperties(String name) {
        return getEndpointProperties(NetworkNodeEnum.orderer, name);
    }

    public Properties getPeerProperties(String name) {
        return getEndpointProperties(NetworkNodeEnum.peer, name);
    }

    private Properties getEndpointProperties(final NetworkNodeEnum type, String name) {
        String path = networkPath + "/network/crypto-config";
        final String domainName = getDomainName(name);
        File cert = Paths.get(path, "/ordererOrganizations".replace("orderer", type.name()),
            domainName, type + "s", name, "tls/server.crt").toFile();
        if (!cert.exists()) {
            throw new RuntimeException(String.format("Missing cert file for: %s. Could not find at location: %s",
                name, cert.getAbsolutePath()));
        }

        Properties prop = new Properties();
        prop.put("pemFile", cert.getAbsolutePath());
        if (isTls()) {
            File clientCert = Paths.get(path, "/ordererOrganizations".replace("orderer", type.name()),
                domainName, "users/Admin@" + domainName, "/tls/client.crt").toFile();
            File clientKey = Paths.get(path, "/ordererOrganizations".replace("orderer", type.name()),
                domainName, "users/Admin@" + domainName, "tls/client.key").toFile();
            prop.put("clientCertFile", clientCert.getAbsolutePath());
            prop.put("clientKeyFile", clientKey.getAbsolutePath());
        }
        prop.put("hostnameOverride", name);
        prop.put("sslProvider", "openSSL");
        prop.put("negotiationType", "TLS");
        if ("orderer".equals(type)) {
            prop.put("ordererWaitTimeMilliSecs", "300000");
            prop.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[]{5L, TimeUnit.MINUTES});
            prop.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[]{8L, TimeUnit.SECONDS});
            prop.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[]{true});
        }
        prop.put("grpc.ManagedChannelBuilderOption.maxInboundMessageSize", 9000000);
        return prop;
    }

    public UserContext getEnrollAdminUser(PeerOrg adminPeerOrg) throws Exception {
        String domainName = getDomainName(getChaincodeInfo().getChaincodePeerName());
        String peerMspPath = String.format(
            "%s/network/crypto-config/peerOrganizations/%s/users/Admin@%s/msp/",
            networkPath, domainName, domainName);
        File adminPkFile = findFileSk(Paths.get(String.format("%s/keystore", peerMspPath)).toFile());
        File adminCertFile = Paths.get(String.format("%s/signcerts/Admin@%s-cert.pem", peerMspPath, domainName))
            .toFile();

        Enrollment enrollOrgAdmin = UserContextUtil.getEnrollment(adminPkFile, adminCertFile);
        UserContext adminUser = new UserContext();
        adminUser.setName(Constant.USERADMIN);
        adminUser.setAffiliation(adminPeerOrg.getOrgName());
        adminUser.setMspid(adminPeerOrg.getOrgMspId());
        adminUser.setEnrollment(enrollOrgAdmin);
        return adminUser;
    }

    /**
     * 根据当前网络配置中的chaincodePeerName查找对应的Org信息
     *
     * @param peerName 节点名的全称
     * @return 节点信息和对应的Org信息
     */
    public PeerOrg getChaincodeAdminPeerOrg(String peerName) throws Exception {
        for (PeerOrg peerOrg : peerOrgsList) {
            for (PeerUnit peer : peerOrg.getPeers()) {
                if (peer.getPeerName().equals(peerName)) {
                    PeerOrg tmpPeerOrg = new PeerOrg();
                    tmpPeerOrg.setOrgName(peerOrg.getOrgName());
                    tmpPeerOrg.setOrgMspId(peerOrg.getOrgMspId());
                    tmpPeerOrg.setPeers(Arrays.asList(peer));
                    return tmpPeerOrg;
                }
            }
        }
        throw new Exception(String.format("Can't find org for %s", peerName));
    }

    /**
     * 根据配置的Orderer名称查找对应的Orderer信息
     */
    public OrdererUnit getChaincodeOrderUnit() throws Exception {
        String chaincodeOrdererName = getChaincodeInfo().getChaincodeOrdererName();
        for (OrdererUnit ordererUnit : ordererList) {
            if (ordererUnit.getOrdererName().equals(chaincodeOrdererName)) {
                return ordererUnit;
            }
        }
        throw new Exception(String.format("Can't find orderer for %s", chaincodeOrdererName));
    }

    public PeerUnit getChaincodePeer(String peerName) throws Exception {
        for (PeerOrg peerOrg : peerOrgsList) {
            for (PeerUnit peer : peerOrg.getPeers()) {
                if (peer.getPeerName().equals(peerName)) {
                    return peer;
                }
            }
        }
        throw new Exception(String.format("", peerName));
    }
}
