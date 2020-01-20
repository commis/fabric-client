package com.energy.justsdk.model;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * @author Bryan
 * @date 2020-01-14
 */

public class FabricResult {

    private final String STATUS = "state";
    private final String PAYLOAD = "payload";
    private final String MESSAGE = "message";

    @Getter
    private Map<String, Object> response;

    public FabricResult() {
        response = new HashMap<>();
    }

    public FabricResult failed(int code, String message) {
        response.put(STATUS, code);
        response.put(MESSAGE, message);
        return this;
    }

    public FabricResult success(String payload) {
        response.put(STATUS, FabricStateCode.SUCCESS);
        response.put(PAYLOAD, payload);
        response.put(MESSAGE, "success");
        return this;
    }
}
