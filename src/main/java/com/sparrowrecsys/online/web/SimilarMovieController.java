package com.sparrowrecsys.online.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparrowrecsys.online.datamanager.Movie;
import com.sparrowrecsys.online.service.DataManager;
import com.sparrowrecsys.online.service.SimilarMovieService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Slf4j
public class SimilarMovieController {
    @Resource
    private DataManager dataManager;
    @Resource
    private SimilarMovieService similarMovieService;

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/getsimilarmovie",  produces = "application/json")
    public String rec(@RequestParam("movieId") String movieId, @RequestParam("size") String size, @RequestParam("model") String model) {
        String jsonMovies = "";

        //use SimilarMovieFlow to get similar movies
        List<Movie> movies = similarMovieService.getRecList(Integer.parseInt(movieId), Integer.parseInt(size), model);

        //convert movie list to json format and return
        ObjectMapper mapper = new ObjectMapper();
        try {
            jsonMovies = mapper.writeValueAsString(movies);
        } catch (JsonProcessingException e) {
            log.warn("Error while converting movie list to json format");
        }
        return jsonMovies;
    }
}
