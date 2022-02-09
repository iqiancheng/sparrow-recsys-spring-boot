package com.sparrowrecsys.online.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparrowrecsys.online.datamanager.User;
import com.sparrowrecsys.online.service.DataManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Slf4j
public class UserController {
    @Resource
    private DataManager dataManager;

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/getuser",  produces = "application/json")
    public String getUser(@RequestParam("id") String userId) {
        String userJson = "";
        User user = dataManager.getUserById(Integer.parseInt(userId));
        //convert movie object to json format and return
        if (null != user) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                userJson = mapper.writeValueAsString(user);
            } catch (JsonProcessingException e) {
                log.warn("Error while converting user object to json format");
            }
        }
        return userJson;
    }
}
