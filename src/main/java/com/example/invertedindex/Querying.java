package com.example.invertedindex;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class Querying {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Querying <query>");
            System.exit(1);
        }

        var query = args[0];
        var invertedIndexFile = Path.of("data", "inverted_index.ser").toString();
        var stopWords = Set.of("a", "an", "and", "the", "is", "in", "at", "of", "on");

        var invertedIndex = loadInvertedIndex(invertedIndexFile);
        var dictionary = invertedIndex.keySet();

        var words = preprocessQuery(query, stopWords, dictionary);
        var result = executeQuery(words, invertedIndex);

        System.out.println(result);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Set<String>> loadInvertedIndex(String filepath) {
        try (var in = new ObjectInputStream(new FileInputStream(filepath))) {
            return (Map<String, Set<String>>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("ERROR: Could not load inverted index from file " + filepath);
            System.exit(1);
        }
        return Map.of();
    }

    private static Set<String> preprocessQuery(String query, Set<String> stopWords, Set<String> dictionary) {
        query = query.toLowerCase().replaceAll("[^A-Za-z']+", " ");
        return Arrays.stream(query.split(" "))
                .filter(word -> !stopWords.contains(word) && dictionary.contains(word))
                .collect(Collectors.toSet());
    }

    private static Set<String> executeQuery(Set<String> words, Map<String, Set<String>> invertedIndex) {
        var result = new HashSet<String>();
        for (var word : words) {
            if (result.isEmpty()) {
                result = new HashSet<>(invertedIndex.getOrDefault(word, Set.of()));
            } else {
                result.retainAll(invertedIndex.getOrDefault(word, Set.of()));
            }
        }
        return result;
    }
}
