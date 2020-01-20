package com.energy.justsdk.service.impl;

import com.energy.justsdk.model.FabricResult;
import com.energy.justsdk.service.ClientService;
import com.energy.justsdk.service.client.FabricHelper;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Bryan
 * @date 2019-12-06
 */
@Slf4j
@Service
public class ClientServiceImpl implements ClientService {

    @Override
    public Map<String, Object> queryBlock(int blockSize) {
        FabricHelper helper = FabricHelper.getInstance();
        FabricResult result = helper.queryBlock(blockSize);

        return result.getResponse();
    }

    @Override
    public Map<String, Object> queryTransaction(String txId) {
        FabricHelper helper = FabricHelper.getInstance();
        FabricResult result = helper.queryTransactionByID(txId);

        return result.getResponse();
    }

    @Override
    public Map<String, Object> query(String fcn, String[] args) {
        FabricHelper helper = FabricHelper.getInstance();
        FabricResult result = helper.queryByChainCode(fcn, args);

        return result.getResponse();
    }

    @Override
    public Map<String, Object> invoke(String fcn, String[] args) {
        FabricHelper helper = FabricHelper.getInstance();
        FabricResult result = helper.invokeByChainCode(fcn, args);

        return result.getResponse();
    }
}
