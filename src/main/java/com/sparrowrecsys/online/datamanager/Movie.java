package com.sparrowrecsys.online.datamanager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sparrowrecsys.online.model.Embedding;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Movie Class, contains attributes loaded from movielens movies.csv and other advanced data like averageRating, emb, etc.
 */
@Data
public class Movie {
    final int TOP_RATING_SIZE = 10;
    int movieId;
    String title;
    int releaseYear;
    String imdbId;
    String tmdbId;
    List<String> genres;
    //how many user rate the movie
    int ratingNumber;
    //average rating score
    double averageRating;
    //embedding of the movie
    @JsonIgnore
    Embedding emb;
    //all rating scores list
    @JsonIgnore
    List<Rating> ratings;
    @JsonIgnore
    Map<String, String> movieFeatures;
    @JsonSerialize(using = RatingListSerializer.class)
    List<Rating> topRatings;

    public Movie() {
        ratingNumber = 0;
        averageRating = 0;
        this.genres = new ArrayList<>();
        this.ratings = new ArrayList<>();
        this.topRatings = new LinkedList<>();
        this.emb = null;
        this.movieFeatures = null;
    }

    public void addGenre(String genre) {
        this.genres.add(genre);
    }

    public void addRating(Rating rating) {
        averageRating = (averageRating * ratingNumber + rating.getScore()) / (ratingNumber + 1);
        ratingNumber++;
        this.ratings.add(rating);
        addTopRating(rating);
    }

    public void addTopRating(Rating rating) {
        if (this.topRatings.isEmpty()) {
            this.topRatings.add(rating);
        } else {
            int index = 0;
            for (Rating topRating : this.topRatings) {
                if (topRating.getScore() >= rating.getScore()) {
                    break;
                }
                index++;
            }
            topRatings.add(index, rating);
            if (topRatings.size() > TOP_RATING_SIZE) {
                topRatings.remove(0);
            }
        }
    }

}
