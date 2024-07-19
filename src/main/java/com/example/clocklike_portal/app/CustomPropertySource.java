package com.example.clocklike_portal.app;

import org.springframework.core.env.EnumerablePropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class CustomPropertySource extends EnumerablePropertySource<Object> {

    private final Map<String, String> properties = new HashMap<>();

    public CustomPropertySource(String name, String filePath) {
        super(name);
        try {
            try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
                lines.forEach(line -> {
                    String[] split = line.split("=");
                    if (split.length == 2) {
                        properties.put(split[0].trim(), split[1].trim());
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties from file: " + filePath, e);
        }
    }

    @Override
    public String[] getPropertyNames() {
        return properties.keySet().toArray(new String[0]);
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }
}