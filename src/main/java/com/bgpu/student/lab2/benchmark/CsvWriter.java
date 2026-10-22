package com.bgpu.student.lab2.benchmark;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class CsvWriter {

    private CsvWriter() {
    }

    public static void write(Path file, List<Benchmark.Result> results)
            throws IOException {

        Files.createDirectories(file.getParent());

        try (BufferedWriter bw = Files.newBufferedWriter(file);
             ICSVWriter csv = new CSVWriterBuilder(bw)
                     .withSeparator(',')
                     .withQuoteChar(ICSVWriter.NO_QUOTE_CHARACTER)
                     .withLineEnd("\n")
                     .build()) {

            csv.writeNext(new String[]{
                    "variant", "threads", "time_seconds", "correct"
            });

            for (Benchmark.Result r : results) {
                csv.writeNext(new String[]{
                        r.variant(),
                        Integer.toString(r.threads()),
                        String.format(Locale.ROOT, "%.4f", r.medianSeconds()),
                        Boolean.toString(r.correct())
                });
            }
        }
    }
}