package com.sparrowrecsys.online.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparrowrecsys.online.datamanager.Movie;
import com.sparrowrecsys.online.service.DataManager;
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
public class RecommendationController {

    @Resource
    private DataManager dataManager;

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/getrecommendation",  produces = "application/json")
    public String rec(@RequestParam("genre") String genre, @RequestParam("size") String size, @RequestParam("sortby") String sortby) {
        String jsonMovies = "";
        //a simple method, just fetch all the movie in the genre
        List<Movie> movies = dataManager.getMoviesByGenre(genre, Integer.parseInt(size), sortby);
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
