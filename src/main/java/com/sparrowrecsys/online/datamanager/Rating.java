package com.sparrowrecsys.online.datamanager;

import lombok.Data;

/**
 * Rating Class, contains attributes loaded from movielens ratings.csv
 */
@Data
public class Rating {
    int movieId;
    int userId;
    float score;
    long timestamp;

}
