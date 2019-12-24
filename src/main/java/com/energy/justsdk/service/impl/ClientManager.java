package com.energy.justsdk.service.impl;

import com.energy.justsdk.service.client.ChannelClient;
import com.energy.justsdk.service.client.FabricClient;
import com.energy.justsdk.service.config.NetworkProperty;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.Channel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author Bryan
 * @date 2019-12-23
 */
@Slf4j
@Service
public class ClientManager {

    @Getter
    private NetworkProperty property;
    private ConcurrentHashMap<String, ChannelClient> channelClientMap;

    @Value("${server.network}")
    private String networkPath;

    @PostConstruct
    @SuppressWarnings("unchecked")
    void afterConstruct() throws Exception {
        String fixture = Objects.requireNonNull(ClientHandler.class.getClassLoader().getResource("fixture")).getFile();
        if (!StringUtils.isEmpty(fixture)) {
            log.info("sdk config path: {}", fixture);
            try {
                String chainNetwork = fixture + "/conf";
                if (!new File(chainNetwork).exists()) {
                    chainNetwork = networkPath;
                    if (!new File(chainNetwork).exists()) {
                        throw new Exception(
                            String.format("The chain crypto config path '%s' is not exist.", chainNetwork));
                    }
                }
                this.channelClientMap = new ConcurrentHashMap();
                this.property = new NetworkProperty(fixture, chainNetwork);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            throw new Exception(String.format("Can't find sdk config in path %s", fixture));
        }
    }

    public ChannelClient getChannelClient(String channelName, String peerName) throws Exception {
        try {
            ChannelClient channelClient = channelClientMap.get(channelName);
            if (channelClient == null) {
                FabricClient fabClient = new FabricClient(property, peerName);
                channelClient = fabClient.createChannelClient(property, channelName);
                channelClientMap.put(channelName, channelClient);
            }
            return channelClient;
        } catch (Exception e) {
            String message = String.format("Get channel client exception | %s", e.getMessage());
            log.error(message);
            throw new Exception(message);
        }
    }

    public boolean isExist(String channelName) {
        ChannelClient channelClient = channelClientMap.get(channelName);
        return (channelClient != null);
    }

    public void addChannelClient(FabricClient fabClient, String channelName, Channel channel) {
        ChannelClient channelClient = channelClientMap.get(channelName);
        if (channelClient == null) {
            channelClient = new ChannelClient(channelName, channel, fabClient);
            channelClientMap.put(channelName, channelClient);
        }
    }
}
