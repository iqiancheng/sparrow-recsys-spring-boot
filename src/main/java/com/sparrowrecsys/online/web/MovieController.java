package com.sparrowrecsys.online.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparrowrecsys.online.datamanager.Movie;
import com.sparrowrecsys.online.service.DataManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Slf4j
public class MovieController {
    @Resource
    private DataManager dataManager;

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/getmovie",  produces = "application/json")
    public String getMovie(@RequestParam("id") String movieId) {
        String movieJson = "";
        //get movie object from DataManager
        Movie movie = dataManager.getMovieById(Integer.parseInt(movieId));

        //convert movie object to json format and return
        if (null != movie) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                movieJson = mapper.writeValueAsString(movie);
            } catch (JsonProcessingException e) {
                log.warn("Error while converting movie object to json format");
            }
        }
        return movieJson;
    }
}
