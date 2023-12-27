package com.huachuan.mapReduce;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MapReduce {
    public static String map(String path) {
        int lastIndex = path.lastIndexOf('/');
        int lastSecondIndex = path.substring(0, lastIndex).lastIndexOf('/');
        String prefix = path.substring(0, lastSecondIndex);
        String outputFileName = prefix + "/map" + path.substring(lastIndex).split("\\.")[0] + "_map_out.txt"; // 输出文件
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
            List<String> words = new ArrayList<>();
            lines.flatMap(line -> Pattern.compile("\\W+").splitAsStream(line)) // 使用非单词字符作为分隔符
                    .filter(word -> !word.isEmpty()) // 过滤空字符串
                    .forEach(words::add); // 统计单词出现次数

            // 将结果写入文件
            words.forEach((word) -> {
                String content = word + ": " + "1" + "\n";
                try {
                    Files.write(Paths.get(outputFileName), content.getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            System.out.println("统计结果已写入到 " + outputFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFileName;
    }

    public static String reduce(String path) {
        int lastIndex = path.lastIndexOf('/');
        int lastSecondIndex = path.substring(0, lastIndex).lastIndexOf('/');
        String prefix = path.substring(0, lastSecondIndex);
        String outputFileName = prefix + "/reduce" + path.substring(lastIndex).split("\\.")[0] + "_reduce_out.txt"; // 输出文件

        try (Stream<String> lines = Files.lines(Paths.get(path))) {
            Map<String, Long> wordCount = new HashMap<>();

            lines.map(line -> line.split(":")) // 按冒号分割每行
                    .filter(parts -> parts.length == 2) // 确保每行都有正确的格式
                    .forEach(parts -> {
                        String word = parts[0].trim();
                        long count = Long.parseLong(parts[1].trim());
                        wordCount.merge(word, count, Long::sum); // 合并统计
                    });

            // 将结果写入文件
            StringBuilder content = new StringBuilder();
            wordCount.forEach((word, count) -> content.append(word).append(": ").append(count).append("\n"));
            Files.write(Paths.get(outputFileName), content.toString().getBytes());

            System.out.println("统计结果已写入到 " + outputFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFileName;
    }
}
