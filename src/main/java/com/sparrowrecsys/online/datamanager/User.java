package com.sparrowrecsys.online.datamanager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sparrowrecsys.online.model.Embedding;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User class, contains attributes loaded from movielens ratings.csv
 */
@Data
public class User {
    int userId;
    double averageRating = 0;
    double highestRating = 0;
    double lowestRating = 5.0;
    int ratingCount = 0;

    @JsonSerialize(using = RatingListSerializer.class)
    List<Rating> ratings;

    //embedding of the movie
    @JsonIgnore
    Embedding emb;

    @JsonIgnore
    Map<String, String> userFeatures;

    public User() {
        this.ratings = new ArrayList<>();
        this.emb = null;
        this.userFeatures = null;
    }


    public void addRating(Rating rating) {
        this.ratings.add(rating);
        this.averageRating = (this.averageRating * ratingCount + rating.getScore()) / (ratingCount + 1);
        if (rating.getScore() > highestRating) {
            highestRating = rating.getScore();
        }

        if (rating.getScore() < lowestRating) {
            lowestRating = rating.getScore();
        }

        ratingCount++;
    }

}
