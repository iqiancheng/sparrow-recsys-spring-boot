package com.sparrowrecsys.online.service;

import com.sparrowrecsys.online.datamanager.Movie;
import com.sparrowrecsys.online.datamanager.Rating;
import com.sparrowrecsys.online.datamanager.RedisClient;
import com.sparrowrecsys.online.datamanager.User;
import com.sparrowrecsys.online.util.Config;
import com.sparrowrecsys.online.util.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * DataManager is a utility class, takes charge of all data loading logic.
 */
@Service
@Slf4j
public class DataManager {
    //singleton instance
    final HashMap<Integer, Movie> movieMap = new HashMap<>();
    final HashMap<Integer, User> userMap= new HashMap<>();
    //genre reverse index for quick querying all movies in a genre
    final HashMap<String, List<Movie>> genreReverseIndexMap = new HashMap<>();

    @PostConstruct
    private void init(){
        //load all the data to DataManager
        try {
            loadData( "/data/sampledata/movies.csv",
                    "/data/sampledata/links.csv", "/data/sampledata/ratings.csv",
                    "/data/modeldata/item2vecEmb.csv",
                    "/data/modeldata/userEmb.csv",
                    "i2vEmb", "uEmb");
        } catch (Exception e) {
            log.warn("Failed to load data from file system. " , e);
        }
    }

    //load data from file system including movie, rating, link data and model data like embedding vectors.
    public void loadData(String movieDataPath, String linkDataPath, String ratingDataPath, String movieEmbPath, String userEmbPath, String movieRedisKey, String userRedisKey) throws Exception {
        loadMovieData(movieDataPath);
        loadLinkData(linkDataPath);
        loadRatingData(ratingDataPath);
        loadMovieEmb(movieEmbPath, movieRedisKey);
        if (Config.IS_LOAD_ITEM_FEATURE_FROM_REDIS) {
            loadMovieFeatures();
        }

        loadUserEmb(userEmbPath);
    }

    //load movie data from movies.csv
    private void loadMovieData(String movieDataPath) {
        log.info("Loading movie data from {} ...", movieDataPath);
        boolean skipFirstLine = true;
        try (Scanner scanner = new Scanner(Objects.requireNonNull(this.getClass().getResourceAsStream(movieDataPath)))) {
            while (scanner.hasNextLine()) {
                String movieRawData = scanner.nextLine();
                if (skipFirstLine) {
                    skipFirstLine = false;
                    continue;
                }
                String[] movieData = movieRawData.split(",");
                if (movieData.length == 3) {
                    Movie movie = new Movie();
                    movie.setMovieId(Integer.parseInt(movieData[0]));
                    int releaseYear = parseReleaseYear(movieData[1].trim());
                    if (releaseYear == -1) {
                        movie.setTitle(movieData[1].trim());
                    } else {
                        movie.setReleaseYear(releaseYear);
                        movie.setTitle(movieData[1].trim().substring(0, movieData[1].trim().length() - 6).trim());
                    }
                    String genres = movieData[2];
                    if (!genres.trim().isEmpty()) {
                        String[] genreArray = genres.split("\\|");
                        for (String genre : genreArray) {
                            movie.addGenre(genre);
                            addMovie2GenreIndex(genre, movie);
                        }
                    }
                    this.movieMap.put(movie.getMovieId(), movie);
                }
            }
        }
        log.info("Loading movie data completed. " + this.movieMap.size() + " movies in total.");
    }

    //load movie embedding
    private void loadMovieEmb(String movieEmbPath, String embKey) {
        if (Config.EMB_DATA_SOURCE.equals(Config.DATA_SOURCE_FILE)) {
            log.info("Loading movie embedding from {} ...", movieEmbPath);
            int validEmbCount = 0;
            try (Scanner scanner = new Scanner(Objects.requireNonNull(this.getClass().getResourceAsStream(movieEmbPath)))) {
                while (scanner.hasNextLine()) {
                    String movieRawEmbData = scanner.nextLine();
                    String[] movieEmbData = movieRawEmbData.split(":");
                    if (movieEmbData.length == 2) {
                        Movie m = getMovieById(Integer.parseInt(movieEmbData[0]));
                        if (null == m) {
                            continue;
                        }
                        m.setEmb(Utility.parseEmbStr(movieEmbData[1]));
                        validEmbCount++;
                    }
                }
            }
            log.info("Loading movie embedding completed. " + validEmbCount + " movie embeddings in total.");
        } else {
            log.info("Loading movie embedding from Redis ...");
            Set<String> movieEmbKeys = RedisClient.getInstance().keys(embKey + "*");
            int validEmbCount = 0;
            for (String movieEmbKey : movieEmbKeys) {
                String movieId = movieEmbKey.split(":")[1];
                Movie m = getMovieById(Integer.parseInt(movieId));
                if (null == m) {
                    continue;
                }
                m.setEmb(Utility.parseEmbStr(RedisClient.getInstance().get(movieEmbKey)));
                validEmbCount++;
            }
            log.info("Loading movie embedding completed. " + validEmbCount + " movie embeddings in total.");
        }
    }

    //load movie features
    private void loadMovieFeatures() {
        log.info("Loading movie features from Redis ...");
        Set<String> movieFeaturesKeys = RedisClient.getInstance().keys("mf:" + "*");
        int validFeaturesCount = 0;
        for (String movieFeaturesKey : movieFeaturesKeys) {
            String movieId = movieFeaturesKey.split(":")[1];
            Movie m = getMovieById(Integer.parseInt(movieId));
            if (null == m) {
                continue;
            }
            m.setMovieFeatures(RedisClient.getInstance().hgetAll(movieFeaturesKey));
            validFeaturesCount++;
        }
        log.info("Loading movie features completed. " + validFeaturesCount + " movie features in total.");
    }

    //load user embedding
    private void loadUserEmb(String userEmbPath) {
        if (Config.EMB_DATA_SOURCE.equals(Config.DATA_SOURCE_FILE)) {
            log.info("Loading user embedding from {} ...", userEmbPath);
            int validEmbCount = 0;
            try (Scanner scanner = new Scanner(Objects.requireNonNull(this.getClass().getResourceAsStream(userEmbPath)))) {
                while (scanner.hasNextLine()) {
                    String userRawEmbData = scanner.nextLine();
                    String[] userEmbData = userRawEmbData.split(":");
                    if (userEmbData.length == 2) {
                        User u = getUserById(Integer.parseInt(userEmbData[0]));
                        if (null == u) {
                            continue;
                        }
                        u.setEmb(Utility.parseEmbStr(userEmbData[1]));
                        validEmbCount++;
                    }
                }
            }
            log.info("Loading user embedding completed. " + validEmbCount + " user embeddings in total.");
        }
    }

    //parse release year
    private int parseReleaseYear(String rawTitle) {
        if (null == rawTitle || rawTitle.trim().length() < 6) {
            return -1;
        } else {
            String yearString = rawTitle.trim().substring(rawTitle.length() - 5, rawTitle.length() - 1);
            try {
                return Integer.parseInt(yearString);
            } catch (NumberFormatException exception) {
                return -1;
            }
        }
    }

    //load links data from links.csv
    private void loadLinkData(String linkDataPath) {
        log.info("Loading link data from  {} ...", linkDataPath);
        int count = 0;
        boolean skipFirstLine = true;
        try (Scanner scanner = new Scanner(Objects.requireNonNull(this.getClass().getResourceAsStream(linkDataPath)))) {
            while (scanner.hasNextLine()) {
                String linkRawData = scanner.nextLine();
                if (skipFirstLine) {
                    skipFirstLine = false;
                    continue;
                }
                String[] linkData = linkRawData.split(",");
                if (linkData.length == 3) {
                    int movieId = Integer.parseInt(linkData[0]);
                    Movie movie = this.movieMap.get(movieId);
                    if (null != movie) {
                        count++;
                        movie.setImdbId(linkData[1].trim());
                        movie.setTmdbId(linkData[2].trim());
                    }
                }
            }
        }
        log.info("Loading link data completed. " + count + " links in total.");
    }

    //load ratings data from ratings.csv
    private void loadRatingData(String ratingDataPath) {
        log.info("Loading rating data from {} ...", ratingDataPath);
        boolean skipFirstLine = true;
        int count = 0;
        try (Scanner scanner = new Scanner(Objects.requireNonNull(this.getClass().getResourceAsStream(ratingDataPath)))) {
            while (scanner.hasNextLine()) {
                String ratingRawData = scanner.nextLine();
                if (skipFirstLine) {
                    skipFirstLine = false;
                    continue;
                }
                String[] linkData = ratingRawData.split(",");
                if (linkData.length == 4) {
                    count++;
                    Rating rating = new Rating();
                    rating.setUserId(Integer.parseInt(linkData[0]));
                    rating.setMovieId(Integer.parseInt(linkData[1]));
                    rating.setScore(Float.parseFloat(linkData[2]));
                    rating.setTimestamp(Long.parseLong(linkData[3]));
                    Movie movie = this.movieMap.get(rating.getMovieId());
                    if (null != movie) {
                        movie.addRating(rating);
                    }
                    if (!this.userMap.containsKey(rating.getUserId())) {
                        User user = new User();
                        user.setUserId(rating.getUserId());
                        this.userMap.put(user.getUserId(), user);
                    }
                    this.userMap.get(rating.getUserId()).addRating(rating);
                }
            }
        }

        log.info("Loading rating data completed. " + count + " ratings in total.");
    }

    //add movie to genre reversed index
    private void addMovie2GenreIndex(String genre, Movie movie) {
        if (!this.genreReverseIndexMap.containsKey(genre)) {
            this.genreReverseIndexMap.put(genre, new ArrayList<>());
        }
        this.genreReverseIndexMap.get(genre).add(movie);
    }

    //get movies by genre, and order the movies by sortBy method
    public List<Movie> getMoviesByGenre(String genre, int size, String sortBy) {
        if (null != genre) {
            List<Movie> movies = new ArrayList<>(this.genreReverseIndexMap.get(genre));
            switch (sortBy) {
                case "rating":
                    movies.sort((m1, m2) -> Double.compare(m2.getAverageRating(), m1.getAverageRating()));
                    break;
                case "releaseYear":
                    movies.sort((m1, m2) -> Integer.compare(m2.getReleaseYear(), m1.getReleaseYear()));
                    break;
                default:
            }

            if (movies.size() > size) {
                return movies.subList(0, size);
            }
            return movies;
        }
        return null;
    }

    //get top N movies order by sortBy method
    public List<Movie> getMovies(int size, String sortBy) {
        List<Movie> movies = new ArrayList<>(movieMap.values());
        switch (sortBy) {
            case "rating":
                movies.sort((m1, m2) -> Double.compare(m2.getAverageRating(), m1.getAverageRating()));
                break;
            case "releaseYear":
                movies.sort((m1, m2) -> Integer.compare(m2.getReleaseYear(), m1.getReleaseYear()));
                break;
            default:
        }

        if (movies.size() > size) {
            return movies.subList(0, size);
        }
        return movies;
    }

    //get movie object by movie id
    public Movie getMovieById(int movieId) {
        return this.movieMap.get(movieId);
    }

    //get user object by user id
    public User getUserById(int userId) {
        return this.userMap.get(userId);
    }
}
