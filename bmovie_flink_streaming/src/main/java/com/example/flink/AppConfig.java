package com.example.flink;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AppConfig implements Serializable {
    private final Map<String, String> map = new HashMap<>();

    public void addProperty(String key, String val) {
        map.put(key, val);
    }

    public String getProperty(String key) {
        return map.get(key);
    }
}
