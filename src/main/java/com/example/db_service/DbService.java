package com.example.db_service;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DbService {
    @Autowired
    private DbFileService dbFileService;

    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    public void executeQuery(String query) throws InvalidQueryException {

            if (query.trim().toUpperCase().startsWith("CREATE TABLE")) {
                createTable(query);
            } else if (query.trim().toUpperCase().startsWith("INSERT INTO")) {
                insertIntoTable(query);
            } else {
                throw new InvalidQueryException("Invalid Operation");
            }
            redisTemplate.opsForValue().increment("SUCCESS");
    }

    private void createTable(String query)  {
        dbFileService.writeMetadata(query);
    }

    private void insertIntoTable(String query) {
        dbFileService.writeData(query);
    }
}
