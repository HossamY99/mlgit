package com.wavemark.noc.sentryeventsexporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

public class FileUtil {

    private final static Logger log = LoggerFactory.getLogger(FileUtil.class);

    public static void moveFiles(File[] files, String sourceDir, String outDir) {
        final File sourceDirectory = new File(sourceDir + File.separator);
        final File destinationDir = new File(outDir + File.separator);
        for (File f : files) {
            Path sourcePath = Paths.get(sourceDirectory.getAbsolutePath() + File.separator + f.getName());
            Path destinationPath = Paths.get(destinationDir.getAbsolutePath() + File.separator + f.getName());

            try {
                Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error(String.valueOf(e));
            }
        }
    }


    public static void removeFirstLineFromFile(File myFile) {
        try {
            Scanner fileScanner = new Scanner(myFile);
            fileScanner.nextLine();

            FileWriter fileStream = new FileWriter(myFile);
            BufferedWriter fileOut = new BufferedWriter(fileStream);
            while (fileScanner.hasNextLine()) {
                String next = fileScanner.nextLine();
                if (next.equals("\n"))
                    fileOut.newLine();
                else
                    fileOut.write(next);
                fileOut.newLine();
            }
            fileOut.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}
