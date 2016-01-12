package com.yo1000.daisychain;

import com.yo1000.daisychain.counter.BranchCounter;
import com.yo1000.daisychain.counter.ReferenceCounter;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by yoichi.kikuchi on 2016/01/12.
 */
public class CountSummary {
    public static void main(String[] args) throws IOException {
        String directoryPath = args.length >= 1 ? "file:" + args[0] : null;
        String filterPackage = args.length >= 2 ? args[1] : ".*";

        Path directory = Paths.get(URI.create(directoryPath));

        Map<String, Integer> referenceCounter = new ReferenceCounter().count(directory);
        Map<String, Integer> branchCounter = new BranchCounter().count(directory);

        Map<String, Count> summary = new TreeMap<String, Count>();

        referenceCounter.entrySet().stream().forEach(entry -> {
            summary.put(entry.getKey(), new Count(entry.getValue(), 0));
        });

        branchCounter.entrySet().stream().forEach(entry -> {
            Count count = summary.get(entry.getKey());

            if (count == null) {
                summary.put(entry.getKey(), new Count(0, entry.getValue()));
                return;
            }

            count.setBranches(entry.getValue());
            summary.put(entry.getKey(), count);
        });


        summary.entrySet().stream()
                .filter(entry -> entry.getKey().matches(filterPackage))
                .sorted((x, y) -> y.getValue().getBranches() - x.getValue().getBranches())
                .forEach(entry -> System.out.printf("%s %d %d\n",
                        entry.getKey(),
                        entry.getValue().getReferences(),
                        entry.getValue().getBranches()));
    }

    public static class Count {
        private int references;
        private int branches;

        public Count() {}

        public Count(int references, int branches) {
            this.references = references;
            this.branches = branches;
        }

        public int getReferences() {
            return references;
        }

        public void setReferences(int references) {
            this.references = references;
        }

        public int getBranches() {
            return branches;
        }

        public void setBranches(int branches) {
            this.branches = branches;
        }
    }
}
