package com.sparrowrecsys.online.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparrowrecsys.online.datamanager.Movie;
import com.sparrowrecsys.online.service.DataManager;
import com.sparrowrecsys.online.service.RecForYouService;
import com.sparrowrecsys.online.util.ABTest;
import com.sparrowrecsys.online.util.Config;
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
public class RecForYouController {
    @Resource
    private DataManager dataManager;
    @Resource
    private RecForYouService recForYouService;

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/getrecforyou",  produces = "application/json")
    public String rec(@RequestParam("id") String userId, @RequestParam("size") String size, @RequestParam("model") String model) {
        String jsonMovies = "";
        if (Config.IS_ENABLE_AB_TEST) {
            model = ABTest.getConfigByUserId(userId);
        }
        //a simple method, just fetch all the movie in the genre
        List<Movie> movies = recForYouService.getRecList(Integer.parseInt(userId), Integer.parseInt(size), model);

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
