package com.example.db_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DbFileService {

    @Value("${dbservice.metadata.file}")
    private String metadataFile;

    @Value("${dbservice.data.file}")
    private String dataFile;

    private static final String SQL_REGEX =
            "^\\s*CREATE\\s+TABLE\\s*\\(\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s+(INTEGER|STRING)\\s*(,\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s+(INTEGER|STRING)\\s*)*\\)\\s*$";
    private static final String INSERT_REGEX =
            "^\\s*INSERT\\s+into\\s+VALUES\\s*\\(([^)]+)\\)\\s*VALUES\\s*\\(([^)]+)\\)\\s*$";

    public static boolean validateSQLQuery(String query) {
        Pattern pattern = Pattern.compile(SQL_REGEX);
        Matcher matcher = pattern.matcher(query);
        if (!matcher.matches()) {
            return false;
        }

        // check for duplicate column names
        String columnsPart = query.substring(query.indexOf('(') + 1, query.lastIndexOf(')')).trim();
        String[] columns = columnsPart.split(",");

        Set<String> columnNames = new HashSet<>();
        for (String column : columns) {
            String columnName = column.trim().split(" ")[0];
            if (!columnNames.add(columnName)) {
                return false; // Duplicate column name found
            }
        }

        return true;
    }


    public static boolean validateInsertQuery(String query) {
        Pattern pattern = Pattern.compile(INSERT_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(query);
        return matcher.matches();
    }

    public static String reorderValues(String query, String metaData) {

        Pattern pattern = Pattern.compile(INSERT_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(query);
        if (!matcher.matches()) {
            throw new InvalidQueryException("Invalid query format.");
        }

        String[] queryColumns = matcher.group(1).split("\\s*,\\s*");
        String[] queryValues = matcher.group(2).split("\\s*,\\s*");

        if (queryColumns.length != queryValues.length) {
            throw new InvalidQueryException("Column count does not match value count.");
        }


        String[] metaDataParts = metaData.split("\\s*,\\s*");
        Map<String, String> metaDataMap = new HashMap<>();
        for (String part : metaDataParts) {
            String[] columnData = part.split("\\s+");
            if (columnData.length != 2) {
                throw new InvalidQueryException("Invalid metadata format.");
            }
            metaDataMap.put(columnData[0], columnData[1].toUpperCase());
        }


        String[] reorderedValues = new String[metaDataMap.size()];
        Set<String> seenColumns = new HashSet<>();
        for (int i = 0; i < queryColumns.length; i++) {
            String columnName = queryColumns[i].trim();
            String columnValue = queryValues[i].trim();

            if (!metaDataMap.containsKey(columnName)) {
                throw new InvalidQueryException("Column " + columnName + " does not exist in metadata.");
            }
            if (!seenColumns.add(columnName)) {
                throw new InvalidQueryException("Duplicate column name in query: " + columnName);
            }

            String expectedType = metaDataMap.get(columnName);
            if (!isValidType(columnValue, expectedType)) {
                throw new InvalidQueryException("Invalid value type for column " + columnName + ": expected " + expectedType);
            }

            int index = new ArrayList<>(metaDataMap.keySet()).indexOf(columnName);
            reorderedValues[index] = columnValue;
        }

        return String.join(", ", reorderedValues);
    }

    private static boolean isValidType(String value, String type) {
        switch (type) {
            case "STRING":
                return value.matches("^\".*\"$"); // Matches strings enclosed in double quotes
            case "INTEGER":
                return value.matches("^-?\\d+$"); // Matches integer values
            default:
                return false;
        }
    }

    public synchronized void writeMetadata(String query) throws InvalidQueryException{
        if(validateSQLQuery(query)) {
            String metadata = query.substring(query.indexOf("(") + 1, query.indexOf(")")).trim();
            try {
                Files.write(Paths.get(metadataFile), (metadata + System.lineSeparator()).getBytes(), StandardOpenOption.WRITE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        throw new InvalidQueryException("Unsupported query.");
    }

    public synchronized void writeData(String query) throws InvalidQueryException{
        String data="";
        if(validateInsertQuery(query)){
            String metaData = "";
            try {
                metaData = Files.readString(Paths.get(metadataFile)).trim();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            data=reorderValues(query,metaData);
        }else{
            throw new InvalidQueryException("Invalid Query");
        }

        try {
            Files.write(Paths.get(dataFile), (data + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
