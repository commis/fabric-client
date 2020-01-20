package com.energy.justsdk.controller;

import com.energy.justsdk.service.ClientService;
import com.energy.justsdk.util.JsonTools;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Bryan
 * @date 2019-12-09
 */
@Slf4j
@Api(tags = {"区块链应用API"})
@RestController
@RequestMapping(path = "/client")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @ApiOperation(value = "查询区块", notes = "查询区块链区块数据。")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "size", value = "区块数最大值20", defaultValue = "10", required = true, dataType = "int")
    })
    @GetMapping(value = "/block")
    public Map<String, Object> queryBlock(
        @RequestParam Integer size) {
        int blockSize = (size > 20) ? 20 : size;
        return clientService.queryBlock(blockSize);
    }

    @ApiOperation(value = "查询交易", notes = "查询区块链交易数据。")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "txId", value = "交易ID", required = true, dataType = "string")
    })
    @GetMapping(value = "/transaction")
    public Map<String, Object> queryTransaction(
        @RequestParam String txId) {
        return clientService.queryTransaction(txId);
    }

    @ApiOperation(value = "合约查询接口", notes = "调用区块链合约查询接口，查询数据。")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "fcn", value = "函数名", required = true, dataType = "string"),
        @ApiImplicitParam(name = "args", value = "查询参数，格式:{\"a\",\"b\"}", required = true, dataType = "string")
    })
    @GetMapping(value = "/method/{fcn}")
    public Map<String, Object> query(
        @PathVariable String fcn,
        @RequestParam String args) {
        return clientService.query(fcn, JsonTools.fromStringArray(args));
    }

    @ApiOperation(value = "合约调用接口", notes = "调用区块链合约功能执行接口，执行合约。")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "fcn", value = "函数名", required = true, dataType = "string"),
        @ApiImplicitParam(name = "args", value = "查询参数，格式:{\"a\",\"b\",\"5\"}", required = true, dataType = "string")
    })
    @PostMapping(value = "/method/{fcn}")
    public Map<String, Object> invoke(
        @PathVariable String fcn,
        @RequestParam String args) {
        return clientService.invoke(fcn, JsonTools.fromStringArray(args));
    }
}
