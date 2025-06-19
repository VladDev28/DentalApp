package org.example.manager;
import java.io.IOException;

public class Scripts {
    public void runBatchFile(String filePath) {
        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "C:\\postgres_archive\\script.bat");
            builder.inheritIO(); // This makes the batch output appear in the console
            Process process = builder.start();
            process.waitFor(); // Waits for the batch file to finish
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
