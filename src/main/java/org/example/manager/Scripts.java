package org.example.manager;
import java.io.IOException;

public class Scripts {
    public void runBatchFile(String filePath) {
        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "C:\\postgres_archive\\script.bat");
            builder.inheritIO();
            Process process = builder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
