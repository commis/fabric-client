package com.energy.justsdk.service.client;

import com.energy.justsdk.service.config.NetworkProperty;
import com.energy.justsdk.model.OrdererUnit;
import com.energy.justsdk.model.PeerOrg;
import com.energy.justsdk.model.PeerUnit;
import com.energy.justsdk.model.UserContext;
import java.io.File;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.springframework.util.StringUtils;

/**
 * @author Bryan
 * @date 2019-12-18
 */
@Slf4j
@Getter
public class FabricClient {

    private HFClient instance;

    public FabricClient(User context) throws Exception {
        this.instance = HFClient.createNewInstance();
        this.instance.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        this.instance.setUserContext(context);
    }

    public FabricClient(NetworkProperty property, String peerName) throws Exception {
        PeerOrg adminPeerOrg = property.getChaincodeAdminPeerOrg(peerName);
        UserContext adminContext = property.getEnrollAdminUser(adminPeerOrg);
        this.instance = HFClient.createNewInstance();
        this.instance.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        this.instance.setUserContext(adminContext);
    }

    /**
     * 创建一个通道客户端，更新Channel对象信息
     *
     * @param property 网络配置对象
     * @param name 通道名称
     * @param peerName 节点名称，允许为空
     */
    public ChannelClient createChannelClient(NetworkProperty property, String name, String peerName) throws Exception {
        Channel channel = instance.newChannel(name);
        OrdererUnit ordererUnit = property.getChaincodeOrderUnit();
        channel.addOrderer(instance.newOrderer(
            ordererUnit.getOrdererName(),
            ordererUnit.getOrdererUrl(),
            property.getOrdererProperties(ordererUnit.getOrdererName())));

        List<PeerUnit> peerUnits = (!StringUtils.isEmpty(peerName))
            ? property.getChaincodeAdminPeerOrg(peerName).getPeers()
            : property.getJoinedPeers();
        for (PeerUnit peerUnit : peerUnits) {
            channel.addPeer(instance.newPeer(
                peerUnit.getPeerName(),
                peerUnit.getPeerUrl(),
                property.getPeerProperties(peerUnit.getPeerName())));
        }
        channel.initialize();

        ChannelClient client = new ChannelClient(name, channel, this);
        return client;
    }

    /**
     * 部署智能合约
     *
     * @param chainCodeName 智能合约名称
     * @param codePath 智能合约代码路径
     * @param version 智能合约版本
     * @param peers 节点集合
     */
    public Collection<ProposalResponse> deployChainCode(String chainCodeName, String codePath, String version,
        Collection<Peer> peers) throws Exception {
        InstallProposalRequest proposalRequest = instance.newInstallProposalRequest();
        ChaincodeID chaincodeID = ChaincodeID.newBuilder()
            .setName(chainCodeName)
            .setVersion(version).build();
        proposalRequest.setChaincodeID(chaincodeID);
        proposalRequest.setUserContext(instance.getUserContext());
        proposalRequest.setChaincodeSourceLocation(new File(codePath));
        proposalRequest.setChaincodeVersion(version);

        log.info("Deploying chaincode {} using Fabric client {} {}", chainCodeName,
            instance.getUserContext().getMspId(),
            instance.getUserContext().getName());
        Collection<ProposalResponse> proposalResponses = instance.sendInstallProposal(proposalRequest, peers);
        return proposalResponses;
    }
}
