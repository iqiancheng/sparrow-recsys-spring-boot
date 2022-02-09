package com.sparrowrecsys.online.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ABTest {
    final static int trafficSplitNumber = 5;
    final static String bucketAModel = "emb";
    final static String bucketBModel = "neuralcf";
    final static String defaultModel = "emb";

    public static String getConfigByUserId(String userId) {
        if (null == userId || userId.isEmpty()) {
            return defaultModel;
        }

        if (userId.hashCode() % trafficSplitNumber == 0) {
            log.info(userId + " is in bucketA.");
            return bucketAModel;
        } else if (userId.hashCode() % trafficSplitNumber == 1) {
            log.info(userId + " is in bucketB.");
            return bucketBModel;
        } else {
            log.info(userId + " isn't in AB test.");
            return defaultModel;
        }
    }
}
