package com.yo1000.daisychain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yoichi.kikuchi on 2016/01/12.
 */
public class ReferenceCounter {
    protected static final Pattern CLASS_NAME_PATTERN = Pattern.compile("[() ]L([a-zA-Z0-9_\\$/]+);");

    public static void main(String[] args) throws IOException {
        String directoryPath = args.length >= 1 ? "file:" + args[0] : null;
        String filterPackage = args.length >= 2 ? args[1] : "";

        new ReferenceCounter().count(Paths.get(URI.create(directoryPath))).entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(filterPackage))
                .sorted((x, y) -> y.getValue() - x.getValue())
                .forEach(entry -> System.out.printf("%s : %d\n", entry.getKey(), entry.getValue()));
    }

    public Map<String, Integer> count(Path directoryPath) throws IOException {
        Map<String, Integer> dependencies = new TreeMap<>();

        Files.list(directoryPath).forEach(path -> {
            File file = path.toFile();

            if (file.isFile() && file.getName().toLowerCase().endsWith(".class")) {
                try {
                    this.count(file).forEach((key, val) -> {
                        Integer count = dependencies.get(key);
                        dependencies.put(key, count != null ? count + val : val);
                    });
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
            }

            if (file.isDirectory()) {
                try {
                    this.count(path).forEach((key, val) -> {
                        Integer count = dependencies.get(key);
                        dependencies.put(key, count != null ? count + val : val);
                    });
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
            }
        });

        return dependencies;
    }

    public Map<String, Integer> count(File classFile) throws IOException {
        try (FileInputStream stream = new FileInputStream(classFile)) {
            ClassReader reader = new ClassReader(stream);
            StringWriter writer = new StringWriter();

            TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(writer));
            reader.accept(visitor, ClassReader.SKIP_DEBUG);

            String trace = writer.toString();
            Matcher matcher = CLASS_NAME_PATTERN.matcher(trace);

            Map<String, Integer> dependencies = new TreeMap<>();

            while (matcher.find()) {
                String className = matcher.group(1);
                int count = dependencies.containsKey(className) ? dependencies.get(className) : 0;
                dependencies.put(className, count + 1);
            }

            return dependencies;
        }
    }
}
