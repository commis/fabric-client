package com.energy.justsdk.controller;

import com.energy.justsdk.service.BlockChainService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Bryan
 * @date 2019-12-09
 */
@Slf4j
@Api(tags = {"区块链管理API"})
@RestController
@RequestMapping(path = "/blockchain")
public class BlockChainController {

    @Autowired
    private BlockChainService blockChainService;

    @ApiOperation(value = "节点加入通道", notes = "节点加入到区块链通道。")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "channelName", value = "通道名称", required = true, dataType = "string"),
        @ApiImplicitParam(name = "peerName", value = "节点名称", required = true, dataType = "string")
    })
    @PostMapping(value = "/channels/{channelName}")
    public void joinChannel(
        @PathVariable String channelName,
        @RequestParam String peerName) {
        blockChainService.peerJoinChannel(peerName, channelName);
    }

    @ApiOperation(value = "查询通道列表", notes = "查询区块链上的通道名称列表。")
    @GetMapping(value = "/channels")
    public Set<String> queryChannels() {
        return blockChainService.queryAllChannels();
    }

    @ApiOperation(value = "创建通道", notes = "在区块链上创建通道。")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "channelName", value = "函数名", required = true, dataType = "string")
    })
    @PostMapping(value = "/channels")
    public String createChannel(
        @RequestParam String channelName) {
        return blockChainService.createChannel(channelName);
    }

    @ApiOperation(value = "安装智能合约", notes = "在区块链上安装智能合约。")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "peerName", value = "节点全名", required = true, dataType = "string"),
        @ApiImplicitParam(name = "chaincodeName", value = "合约名称", required = true, dataType = "string"),
        @ApiImplicitParam(name = "version", value = "合约版本", required = true, dataType = "string"),
        @ApiImplicitParam(name = "chaincodePath", value = "合约代码路径", required = true, dataType = "string")
    })
    @PostMapping(value = "/peers/{peerName}/chaincodes/{chaincodeName}/{version}")
    public String installChaincode(
        @PathVariable String peerName,
        @PathVariable String chaincodeName,
        @PathVariable String version,
        @RequestParam String chaincodePath) {
        return blockChainService.installChaincode(peerName, chaincodeName, version, chaincodePath);
    }

    @ApiOperation(value = "实例化智能合约", notes = "实例化区块链节点上安装的智能合约。")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "channelName", value = "通道名称", required = true, dataType = "string"),
        @ApiImplicitParam(name = "ordererName", value = "orderer名称", required = true, dataType = "string"),
        @ApiImplicitParam(name = "peerName", value = "节点名称", required = true, dataType = "string"),
        @ApiImplicitParam(name = "chaincodeName", value = "合约名称", required = true, dataType = "string"),
        @ApiImplicitParam(name = "chaincodePath", value = "合约代码路径", required = true, dataType = "string"),
        @ApiImplicitParam(name = "version", value = "合约版本", required = true, dataType = "string"),
        @ApiImplicitParam(name = "args", value = "实例化参数", required = true, dataType = "string"),
        @ApiImplicitParam(name = "endorsePath", value = "endorse地址", required = true, dataType = "string")
    })
    @PutMapping(value = "/peers/{peerName}/chaincodes/{chaincodeName}/{version}")
    public Map<String, String> instantieteChaincode(
        @RequestParam String channelName,
        @RequestParam String ordererName,
        @PathVariable String peerName,
        @PathVariable String chaincodeName,
        @RequestParam String chaincodePath,
        @PathVariable String version,
        @PathVariable String args,
        @RequestParam String endorsePath) {
        /*return blockChainService.instantieteChaincode(channelName, ordererName, peerName,
            chaincodeName, chaincodePath, version, JsonTools.fromStringArray(args), endorsePath);*/
        return null;
    }

    @ApiOperation(value = "升级智能合约", notes = "升级区块链节点上的智能合约，合约使用新的版本号。")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "channelName", value = "通道名称", required = true, dataType = "string"),
        @ApiImplicitParam(name = "ordererName", value = "orderer名称", required = true, dataType = "string"),
        @ApiImplicitParam(name = "peerName", value = "节点名称", required = true, dataType = "string"),
        @ApiImplicitParam(name = "chaincodeName", value = "合约名称", required = true, dataType = "string"),
        @ApiImplicitParam(name = "chaincodePath", value = "合约代码路径", required = true, dataType = "string"),
        @ApiImplicitParam(name = "version", value = "合约版本", required = true, dataType = "string"),
        @ApiImplicitParam(name = "args", value = "实例化参数", required = true, dataType = "string"),
        @ApiImplicitParam(name = "endorsePath", value = "endorse地址", required = true, dataType = "string")
    })
    @PostMapping(value = "/peers/{peerName}/chaincodes/{chaincodeName}/{version}/upgrade")
    public Map<String, String> upgradeChaincode(
        @RequestParam String channelName,
        @RequestParam String ordererName,
        @PathVariable String peerName,
        @PathVariable String chaincodeName,
        @RequestParam String chaincodePath,
        @PathVariable String version,
        @PathVariable String args,
        @RequestParam String endorsePath) {
        /*return blockChainService.upgradeChaincode(channelName, ordererName, peerName,
            chaincodeName, chaincodePath, version, JsonTools.fromStringArray(args), endorsePath);*/
        return null;
    }
}
