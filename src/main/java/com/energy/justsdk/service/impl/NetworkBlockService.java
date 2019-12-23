package com.energy.justsdk.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.energy.justsdk.entity.BlockInfoBean;
import com.energy.justsdk.util.DateTools;
import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.BlockchainInfo;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.TxReadWriteSetInfo;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Bryan
 * @date 2019-12-07
 */
@Slf4j
public class NetworkBlockService {

    public Map<String, Object> getResultJson(JSONArray channelBlockInfos) {
        Map<String, Object> map = new HashMap<>();
        if (!ObjectUtils.isEmpty(channelBlockInfos)) {
            List<BlockInfoBean> detailList = new ArrayList<>();
            int blockHeight = 0;
            int txTotalCount = 0;
            for (int i = 0; i < channelBlockInfos.size(); i++) {
                JSONObject jsonObject = channelBlockInfos.getJSONObject(i);
                if (i == 0) {
                    blockHeight = jsonObject.getIntValue("blockHeight") + 1;
                }
                if (jsonObject.getIntValue("txCount") == 0) {
                    txTotalCount = txTotalCount + 1;
                } else {
                    txTotalCount += jsonObject.getIntValue("txCount");
                }
                BlockInfoBean tlBlockInfo = new BlockInfoBean();
                tlBlockInfo.setBlockHeight(jsonObject.getIntValue("blockHeight"));
                tlBlockInfo.setTxCount(jsonObject.getIntValue("txCount"));
                JSONArray txDetail = jsonObject.getJSONArray("txDetail");
                for (int j = 0; j < txDetail.size(); j++) {
                    JSONObject detailJSONObject = txDetail.getJSONObject(j);
                    String transactionID = detailJSONObject.getString("transactionID");
                    tlBlockInfo.setTxHash(transactionID);
                    tlBlockInfo.setMspid(detailJSONObject.getString("createMSPID"));
                    tlBlockInfo.setChannel(detailJSONObject.getString("channelId"));
                    tlBlockInfo.setTime(detailJSONObject.getString("timestamp"));
                    tlBlockInfo.setDiffTime(transform(detailJSONObject.getString("timestamp")));
                }
                detailList.add(tlBlockInfo);
            }
            map.put("blockHeight", blockHeight);
            map.put("txCount", txTotalCount);
            map.put("detailData", detailList);
        }
        return map;
    }

    private static String transform(String historyDate) {
        historyDate = historyDate.replaceAll("/", "-");
        long second = DateTools.diffDateInSeconds(historyDate, DateTools.getNowTime());
        long day = second / (24 * 60 * 60);
        long remainDay = second % (24 * 60 * 60);
        long hour = 0;
        long min = 0;
        String res = "";
        if (day > 0) {
            res += day + "天";
        }
        hour = (remainDay) / (60 * 60);
        long remainHour = (remainDay) % (60 * 60);
        min = (remainHour) / 60;
        if (hour > 0) {
            res += hour + "小时";
        }
        res += min + "分钟前";
        return res;
    }

    public JSONArray queryPeerBlock(BlockchainInfo blockchainInfo, Channel channel, int blockSize) {
        if (!StringUtils.isEmpty(blockchainInfo)) {
            JSONArray channelBlockInfos = new JSONArray();
            byte[] currentBlockHash = blockchainInfo.getCurrentBlockHash();
            byte[] previousBlockHash;
            int index = 0;
            do {
                try {
                    previousBlockHash = currentBlockHash;
                    BlockInfo preBlockInfo = channel.queryBlockByHash(previousBlockHash);
                    JSONObject preJsonObject = parseBlockInfo(preBlockInfo, Hex.encodeHexString(previousBlockHash));
                    JSONArray preJsonArray = parseEnvelopeInfo(preBlockInfo);
                    preJsonObject.put("txDetail", preJsonArray);
                    channelBlockInfos.add(preJsonObject);
                    currentBlockHash = preBlockInfo.getPreviousHash();
                    index++;
                } catch (Exception e) {
                    log.error("queryPeerBlock exception {}", e.getMessage());
                }
            } while (!ObjectUtils.isEmpty(currentBlockHash) && index < blockSize);
            return channelBlockInfos;
        }
        return null;
    }

    private JSONObject parseBlockInfo(BlockInfo blockInfo, String blockHash) {
        JSONObject blockObj = new JSONObject();
        blockObj.put("blockHash", blockHash);
        //每个块的交易数
        int transactionCount = blockInfo.getTransactionCount();
        blockObj.put("txCount", transactionCount);
        //当前块大小
        long blockNumber = blockInfo.getBlockNumber();
        blockObj.put("blockHeight", blockNumber);
        byte[] dataHash = blockInfo.getDataHash();
        blockObj.put("dataHash", Hex.encodeHexString(dataHash));
        byte[] previousHash = blockInfo.getPreviousHash();
        blockObj.put("previousHash", Hex.encodeHexString(previousHash));
        return blockObj;
    }

    private JSONArray parseEnvelopeInfo(BlockInfo blockInfo)
        throws InvalidProtocolBufferException {
        JSONArray envJsonArray = new JSONArray();
        for (BlockInfo.EnvelopeInfo info : blockInfo.getEnvelopeInfos()) {
            JSONObject json = new JSONObject();
            json.put("channelId", info.getChannelId());
            json.put("transactionID", info.getTransactionID());
            json.put("validationCode", info.getValidationCode());
            json.put("timestamp", DateTools.parseDateFormat(new Date(info.getTimestamp().getTime())));
            json.put("type", info.getType());
            json.put("createMSPID", info.getCreator().getMspid());
            json.put("isValid", info.isValid());

            if (info.getType() == BlockInfo.EnvelopeType.TRANSACTION_ENVELOPE) {
                BlockInfo.TransactionEnvelopeInfo txeInfo = (BlockInfo.TransactionEnvelopeInfo) info;
                JSONObject transactionEnvelopeInfoJson = new JSONObject();
                int txCount = txeInfo.getTransactionActionInfoCount();
                transactionEnvelopeInfoJson.put("txCount", txCount);
                transactionEnvelopeInfoJson.put("isValid", txeInfo.isValid());
                transactionEnvelopeInfoJson.put("validationCode", txeInfo.getValidationCode());
                transactionEnvelopeInfoJson.put("transactionActionInfoArray",
                    getTransactionActionInfoJsonArray(txeInfo, txCount));
                json.put("transactionEnvelopeInfo", transactionEnvelopeInfoJson);
            }
            envJsonArray.add(json);
        }
        return envJsonArray;
    }

    private JSONArray getTransactionActionInfoJsonArray(BlockInfo.TransactionEnvelopeInfo txeInfo, int txCount)
        throws InvalidProtocolBufferException {
        JSONArray transactionActionInfoJsonArray = new JSONArray();
        for (int i = 0; i < txCount; i++) {
            BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo txInfo = txeInfo.getTransactionActionInfo(i);
            int endorsementsCount = txInfo.getEndorsementsCount();
            int chaincodeInputArgsCount = txInfo.getChaincodeInputArgsCount();
            JSONObject transactionActionInfoJson = new JSONObject();
            transactionActionInfoJson.put("responseStatus", txInfo.getResponseStatus());
            transactionActionInfoJson.put("responseMessageString",
                printableString(new String(txInfo.getResponseMessageBytes(), StandardCharsets.UTF_8)));
            transactionActionInfoJson.put("endorsementsCount", endorsementsCount);
            transactionActionInfoJson.put("chaincodeInputArgsCount", chaincodeInputArgsCount);
            transactionActionInfoJson.put("status", txInfo.getProposalResponseStatus());
            transactionActionInfoJson.put("payload",
                printableString(new String(txInfo.getProposalResponsePayload(), StandardCharsets.UTF_8)));
            transactionActionInfoJson.put("endorserInfoArray", getEndorserInfoJsonArray(txInfo, endorsementsCount));

            TxReadWriteSetInfo rwsetInfo = txInfo.getTxReadWriteSet();
            JSONObject rwsetInfoJson = new JSONObject();
            if (null != rwsetInfo) {
                int nsRWsetCount = rwsetInfo.getNsRwsetCount();
                rwsetInfoJson.put("nsRWsetCount", nsRWsetCount);
                rwsetInfoJson.put("nsRwsetInfoArray", getNsRwsetInfoJsonArray(rwsetInfo));
            }
            transactionActionInfoJson.put("rwsetInfo", rwsetInfoJson);
            transactionActionInfoJsonArray.add(transactionActionInfoJson);
        }
        return transactionActionInfoJsonArray;
    }

    private String printableString(final String string) {
        int maxLogStringLength = 64;
        if (string == null || string.length() == 0) {
            return string;
        }
        String ret = string.replaceAll("[^\\p{Print}]", "?");
        ret = ret.substring(0, Math.min(ret.length(), maxLogStringLength)) + (ret.length() > maxLogStringLength ? "..."
            : "");
        return ret;
    }

    /**
     * 解析背书信息
     */
    private JSONArray getEndorserInfoJsonArray(
        BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo txInfo, int endorsementsCount) {
        JSONArray endorserInfoJsonArray = new JSONArray();
        for (int n = 0; n < endorsementsCount; ++n) {
            BlockInfo.EndorserInfo endorserInfo = txInfo.getEndorsementInfo(n);
            JSONObject endorserInfoJson = new JSONObject();
            endorserInfoJson.put("signature", Hex.encodeHexString(endorserInfo.getSignature()));
            endorserInfoJson.put("id", endorserInfo.getId());
            endorserInfoJson.put("mspId", endorserInfo.getMspid());
            endorserInfoJsonArray.add(endorserInfoJson);
        }
        return endorserInfoJsonArray;
    }

    /**
     * 解析读写集集合
     */
    private JSONArray getNsRwsetInfoJsonArray(TxReadWriteSetInfo rwsetInfo) throws InvalidProtocolBufferException {
        JSONArray nsRwsetInfoJsonArray = new JSONArray();
        for (TxReadWriteSetInfo.NsRwsetInfo nsRwsetInfo : rwsetInfo.getNsRwsetInfos()) {
            final String namespace = nsRwsetInfo.getNamespace();
            KvRwset.KVRWSet rws = nsRwsetInfo.getRwset();
            JSONObject nsRwsetInfoJson = new JSONObject();

            nsRwsetInfoJson.put("readSet", getReadSetJSONArray(rws, namespace));
            nsRwsetInfoJson.put("writeSet", getWriteSetJSONArray(rws, namespace));
            nsRwsetInfoJsonArray.add(nsRwsetInfoJson);
        }
        return nsRwsetInfoJsonArray;
    }

    /**
     * 解析读集
     */
    private JSONArray getReadSetJSONArray(KvRwset.KVRWSet rws, String namespace) {
        JSONArray readJsonArray = new JSONArray();
        int rs = -1;
        for (KvRwset.KVRead readList : rws.getReadsList()) {
            rs++;
            String key = readList.getKey();
            long readVersionBlockNum = readList.getVersion().getBlockNum();
            long readVersionTxNum = readList.getVersion().getTxNum();
            JSONObject readInfoJson = new JSONObject();
            readInfoJson.put("namespace", namespace);
            readInfoJson.put("readSetIndex", rs);
            readInfoJson.put("key", key);
            readInfoJson.put("readVersionBlockNum", readVersionBlockNum);
            readInfoJson.put("readVersionTxNum", readVersionTxNum);
            readInfoJson.put("chaincode_version", String.format("[%s : %s]", readVersionBlockNum, readVersionTxNum));
            readJsonArray.add(readInfoJson);
        }
        return readJsonArray;
    }

    /**
     * 解析写集
     */
    private JSONArray getWriteSetJSONArray(KvRwset.KVRWSet rws, String namespace) {
        JSONArray writeJsonArray = new JSONArray();
        int rs = -1;
        for (KvRwset.KVWrite writeList : rws.getWritesList()) {
            rs++;
            String key = writeList.getKey();
            String writeContent = new String(writeList.getValue().toByteArray(), StandardCharsets.UTF_8);
            String valAsString = printableString(
                new String(writeList.getValue().toByteArray(), StandardCharsets.UTF_8));
            JSONObject writeInfoJson = new JSONObject();
            writeInfoJson.put("namespace", namespace);
            writeInfoJson.put("writeSetIndex", rs);
            writeInfoJson.put("key", key);
            writeInfoJson.put("value", writeContent);
            writeJsonArray.add(writeInfoJson);
        }
        return writeJsonArray;
    }

}
