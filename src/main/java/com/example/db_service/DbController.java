package com.example.db_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DbController {

    @Autowired
    private DbService dbService;

    @PostMapping("/execute")
    public ResponseEntity<String> executeQuery(@RequestBody String query){

            dbService.executeQuery(query);
            return ResponseEntity.ok("Query executed successfully.");
    }
}
