package com.example.invertedindex;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class Indexing {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Indexing <docs_path> <data_path>");
            System.exit(1);
        }

        String docsPath = args[0];
        String dataPath = args[1];

        checkDirectory(docsPath);
        checkDirectory(dataPath);

        buildIndex(docsPath, dataPath);
        System.out.println("Indexing completed.");
    }

    private static void checkDirectory(String path) {
        var dir = Path.of(path);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            var errorMsg = !Files.exists(dir) ? "does not exist" : "is not a directory";
            System.out.println("ERROR: " + path + " " + errorMsg);
            System.exit(1);
        }
    }

    private static String cleanText(String text) {
        var replacements = Map.of("’", "'", "“", "\"", "”", "\"");
        text = text.toLowerCase();

        for (var entry : replacements.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }

        return text.replaceAll("[^A-Za-z']+", " ");
    }

    private static Set<String> extractWords(String text) {
        var stopWords = Set.of("a", "an", "and", "the", "is", "in", "at", "of", "on", "your");
        return Arrays.stream(cleanText(text).split(" "))
                .filter(word -> !stopWords.contains(word))
                .collect(Collectors.toSet());
    }

    private static Map<String, Set<String>> createInvertedIndex(String directoryPath) {
        var invertedIndex = new HashMap<String, Set<String>>();
        try (var paths = Files.walk(Path.of(directoryPath))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        var text = readFile(path.toString());
                        var words = extractWords(text);
                        words.forEach(word -> invertedIndex.computeIfAbsent(word, k -> new HashSet<>()).add(path.toString()));
                    });
        } catch (IOException e) {
            System.out.println("ERROR: Could not list directory " + directoryPath);
            System.exit(1);
        }
        return invertedIndex;
    }

    private static String readFile(String filepath) {
        try {
            if (filepath.endsWith(".pdf")) {
                // Using the SimplePDFReader to get text from the PDF
                return readPDF(filepath);
            } else if (filepath.endsWith(".docx")) {
                // Creating a temporary directory to unzip DOCX contents
                String tempDir = "temp_docx";
                try {
                    DocxProcessor.unzipDocx(filepath, tempDir);
                    String content = DocxProcessor.extractTextFromDocx(tempDir);

                    // Clean up temporary directory
                    deleteDirectory(Path.of(tempDir));
                    return content;
                } catch (ParserConfigurationException | SAXException e) {
                    System.out.println("ERROR: XML parsing issue with file " + filepath);
                    e.printStackTrace();
                }
            } else {
                // For plain text files
                return Files.readString(Path.of(filepath), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.out.println("ERROR: Could not read file " + filepath);
            e.printStackTrace();
        }
        return "";
    }

    // This method helps to read the PDF file using PDFBox
    private static String readPDF(String filePath) {
        String text = "";
        try {
            PDDocument document = PDDocument.load(new File(filePath));
            PDFTextStripper pdfStripper = new PDFTextStripper();
            text = pdfStripper.getText(document);
            document.close();
        } catch (IOException e) {
            System.out.println("ERROR: Could not read PDF file " + filePath);
            e.printStackTrace();
        }
        return text;
    }

    private static void buildIndex(String docsDirectory, String dataDirectory) {
        Map<String, Set<String>> invertedIndex = createInvertedIndex(docsDirectory);

        String dictionaryPath = Paths.get(dataDirectory, "dictionary.txt").toString();
        String indexPath = Paths.get(dataDirectory, "inverted_index.ser").toString();

        try (PrintWriter dictWriter = new PrintWriter(new FileWriter(dictionaryPath));
             ObjectOutputStream indexWriter = new ObjectOutputStream(new FileOutputStream(indexPath))) {
            invertedIndex.keySet().forEach(dictWriter::println);
            indexWriter.writeObject(invertedIndex);
        } catch (IOException e) {
            System.out.println("ERROR: Could not write to file " + dictionaryPath + " or " + indexPath);
            System.exit(1);
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (Files.isDirectory(directory)) {
            try (var files = Files.walk(directory)) {
                files.sorted(Comparator.reverseOrder()).forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }
    }
}

