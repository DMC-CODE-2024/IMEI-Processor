package org.example;

import com.gl.custom.CustomCheck;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ImeiProcessor {
    private static final Logger logger = LogManager.getLogger(ImeiProcessor.class);

    @Value("${imei.input.file.path}")
    private String inputDirectoryPath;

    @Value("${imei.output.directory.path}")
    private String outputDirectoryPath;

    public void processImeis() {
        int totalProcessed = 0;
        int totalFailed = 0;
        int emptyLines = 0;

        try {
            File inputFile = detectCsvFile(inputDirectoryPath);
            if (inputFile == null) {
                logger.error("No CSV file found or multiple files detected in the specified directory.");
                return;
            }
            logger.info("Input file detected: {}", inputFile.getName());

            // Move and rename input file to output directory with timestamped name
            String outputFilePath = moveAndRenameFile(inputFile);
            List<String> imeis = Files.readAllLines(Paths.get(outputFilePath));
            logger.info("Processing {} IMEIs from the input file.", imeis.size());

            // Process and write statuses to the same file in the output directory
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
                writer.write("IMEI,Status\n");

                for (String imei : imeis) {
                    if (imei.trim().isEmpty()) {
                        emptyLines++;
                        logger.warn("Skipped empty line in input file.");
                        continue;
                    }

                    String status = null;
                    try {
                        status = CustomCheck.identifyCustomComplianceStatus(imei, "NWL");
                        totalProcessed++;
                    } catch (Exception e) {
                        logger.error("Error fetching compliance status for IMEI {}: {}", imei, e.getMessage());
                        totalFailed++;
                    }
                    writer.write(imei + "," + (status != null ? status : "Error") + "\n");
                }
            }
            logger.info("Processing completed. Output saved to {}", outputFilePath);
            logger.info("Total records processed: {}", totalProcessed);
            logger.info("Total records failed: {}", totalFailed);
            logger.info("Total empty lines skipped: {}", emptyLines);

        } catch (IOException e) {
            logger.error("Error processing IMEIs: {}", e.getMessage());
        }
    }

    private File detectCsvFile(String directoryPath) {
        try {
            List<File> csvFiles = Files.list(Paths.get(directoryPath))
                    .filter(path -> path.toString().endsWith(".csv"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            if (csvFiles.size() == 1) {
                return csvFiles.get(0);
            }
            logger.warn("Expected one CSV file in directory, but found: {}", csvFiles.size());
            return null;
        } catch (IOException e) {
            logger.error("Error detecting CSV file in directory {}: {}", directoryPath, e.getMessage());
            return null;
        }
    }

    private String moveAndRenameFile(File inputFile) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String outputFileName = inputFile.getName().replace(".csv", "_processed_" + timestamp + ".csv");
        Path targetPath = Paths.get(outputDirectoryPath, outputFileName);

        Files.move(inputFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Moved input file to output directory with new name: {}", targetPath);
        return targetPath.toString();
    }
}
