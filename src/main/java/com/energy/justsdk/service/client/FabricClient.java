package com.energy.justsdk.service.client;

import com.energy.justsdk.model.OrdererUnit;
import com.energy.justsdk.model.PeerOrg;
import com.energy.justsdk.model.PeerUnit;
import com.energy.justsdk.model.UserContext;
import com.energy.justsdk.service.config.NetworkProperty;
import java.io.File;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

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
     * 创建通道信息，已经加入了当前配置的Orderer信息
     *
     * @param property 网络配置对象
     * @param name 通道名称
     */
    public Channel createChannel(NetworkProperty property, String name) throws Exception {
        /*String channelTxFile = String.format("%s/network/channel-artifacts/%s.tx",
            property.getNetworkPath(), name);
        ChannelConfiguration configuration = new ChannelConfiguration(new File(channelTxFile));
        byte[] signature = instance.getChannelConfigurationSignature(configuration, instance.getUserContext());*/

        OrdererUnit ordererUnit = property.getChaincodeOrderUnit();
        Orderer orderer = instance.newOrderer(
            ordererUnit.getOrdererName(),
            ordererUnit.getOrdererUrl(),
            property.getOrdererProperties(ordererUnit.getOrdererName()));

        Channel newChannel = instance.newChannel(name/*, orderer, configuration, signature*/);
        newChannel.addOrderer(orderer);

        return newChannel;
    }

    /**
     * 创建一个通道客户端，默认添加链接的Orderer和加入通道的节点(或锚节点)
     *
     * @param property 网络配置对象
     * @param name 通道名称
     */
    public ChannelClient createChannelClient(NetworkProperty property, String name) throws Exception {
        Channel newChannel = instance.newChannel(name);

        OrdererUnit ordererUnit = property.getChaincodeOrderUnit();
        newChannel.addOrderer(instance.newOrderer(
            ordererUnit.getOrdererName(),
            ordererUnit.getOrdererUrl(),
            property.getOrdererProperties(ordererUnit.getOrdererName())));

        List<PeerUnit> peerUnits = property.getJoinedPeers();
        for (PeerUnit peerUnit : peerUnits) {
            newChannel.addPeer(instance.newPeer(
                peerUnit.getPeerName(),
                peerUnit.getPeerUrl(),
                property.getPeerProperties(peerUnit.getPeerName())));
        }
        newChannel.initialize();

        ChannelClient client = new ChannelClient(name, newChannel, this);
        return client;
    }

    /**
     * 部署智能合约
     *
     * @param chainCodeName 智能合约名称
     * @param chaincodePath 智能合约代码路径
     * @param version 智能合约版本
     * @param peers 节点集合
     */
    public Collection<ProposalResponse> deployChainCode(String chainCodeName, String chaincodePath,
        String version, Collection<Peer> peers) throws Exception {
        InstallProposalRequest proposalRequest = instance.newInstallProposalRequest();
        ChaincodeID chaincodeID = ChaincodeID.newBuilder()
            .setName(chainCodeName)
            .setPath(chaincodePath)
            .setVersion(version).build();
        proposalRequest.setChaincodeID(chaincodeID);
        proposalRequest.setUserContext(instance.getUserContext());
        proposalRequest.setChaincodeSourceLocation(new File("chaincode"));
        proposalRequest.setChaincodeVersion(version);

        log.info("Deploying chaincode {} using Fabric client {} {}", chainCodeName,
            instance.getUserContext().getMspId(),
            instance.getUserContext().getName());
        Collection<ProposalResponse> proposalResponses = instance.sendInstallProposal(proposalRequest, peers);
        return proposalResponses;
    }
}
