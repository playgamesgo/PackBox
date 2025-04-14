package me.playgamesgo.packbox.commands;

import lombok.extern.slf4j.Slf4j;
import me.playgamesgo.packbox.utils.Manifest;
import me.playgamesgo.packbox.utils.source.Modrinth;
import org.jetbrains.annotations.Nullable;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import picocli.CommandLine;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(
        name = "install",
        description = "Install files from manifest file"
)
public final class InstallCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            Path manifestFile = Paths.get("manifest.packbox.json");
            if (!Files.exists(manifestFile)) {
                AttributedStringBuilder builder = new AttributedStringBuilder();
                builder.append("Manifest file not found, please run ", AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                        .append("packbox init", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                        .append(" first.", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                System.out.println(builder.toAnsi(terminal));
                return 1;
            }
            Manifest manifest = Manifest.gson.fromJson(new String(Files.readAllBytes(manifestFile)), Manifest.class);
            AttributedStringBuilder builder = new AttributedStringBuilder();
            builder.append("Installing files from manifest file", AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
            System.out.println(builder.toAnsi(terminal));

            int totalMods = manifest.getMods().size();
            var context = new Object() {
                int currentMod = 0;
            };

            for (Manifest.Mod mod : manifest.getMods()) {
                context.currentMod++;
                File modFile = new File(mod.getPath());
                if (modFile.exists() && Optional.ofNullable(Modrinth.createSha1(modFile)).orElseThrow().equals(mod.getSha1())) {
                    updateProgressLine(terminal, mod.getPath(), 100, context.currentMod, totalMods, "SKIPPED");
                    continue;
                }

                try {
                    clearLine(terminal);
                    AttributedStringBuilder startBuilder = new AttributedStringBuilder();
                    startBuilder.append("Starting download: ", AttributedStyle.BOLD)
                            .append(mod.getPath(), AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
                    System.out.println(startBuilder.toAnsi(terminal));

                    URL url = URI.create(mod.getUrl()).toURL();
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    long fileSize = connection.getContentLengthLong();
                    if (fileSize <= 0) fileSize = 1000000; // Default if size unknown

                    if (modFile.getParentFile() != null && !modFile.getParentFile().exists()) {
                        modFile.getParentFile().mkdirs();
                    }

                    try (ProgressTrackingInputStream in = new ProgressTrackingInputStream(
                            connection.getInputStream(), fileSize,
                            (bytesRead, totalBytes) -> updateProgressLine(
                                    terminal, mod.getPath(),
                                    (int) (bytesRead * 100 / totalBytes),
                                    context.currentMod, totalMods, null));
                         FileOutputStream out = new FileOutputStream(modFile)) {

                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }

                    updateProgressLine(terminal, mod.getPath(), 100, context.currentMod, totalMods, "DONE");
                    clearLine(terminal);
                    AttributedStringBuilder successfulBuilder = new AttributedStringBuilder();
                    successfulBuilder.append("Successfully downloaded: ", AttributedStyle.BOLD)
                            .append(mod.getPath(), AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
                    System.out.println(successfulBuilder.toAnsi(terminal));
                } catch (Exception e) {
                    updateProgressLine(terminal, mod.getPath(), 0, context.currentMod, totalMods, "ERROR");
                    log.error("Error downloading file: {}", mod.getUrl(), e);
                }
            }

            clearLine(terminal);
            AttributedStringBuilder complete = new AttributedStringBuilder();
            complete.append("Installation complete: ", AttributedStyle.BOLD)
                    .append(context.currentMod + "/" + totalMods + " mods installed",
                            AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
            System.out.println(complete.toAnsi(terminal));

        } catch (Exception e) {
            log.error("Error initializing terminal", e);
        }

        return 0;
    }

    private void updateProgressLine(Terminal terminal, String modPath, int percent, int currentMod, int totalMods, @Nullable String status) {
        terminal.writer().print("\r");

        String modName = new File(modPath).getName();
        if (modName.length() > 30) {
            modName = modName.substring(0, 27) + "...";
        }

        int overallPercent = (int) ((currentMod - 1) * 100.0 / totalMods + (double) percent / totalMods);

        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.append(String.format("%-30s ", modName), AttributedStyle.BOLD);


        builder.append("[");
        int progressChars = Math.min(50, percent / 2);
        for (int i = 0; i < 50; i++) {
            if (i < progressChars) {
                builder.append("=", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
            } else if (i == progressChars && percent < 100) {
                builder.append(">", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
            } else {
                builder.append(" ");
            }
        }
        builder.append("] ");

        if (status != null) {
            AttributedStyle style = status.equals("DONE") ?
                    AttributedStyle.BOLD.foreground(AttributedStyle.GREEN) :
                    status.equals("ERROR") ?
                            AttributedStyle.BOLD.foreground(AttributedStyle.RED) :
                            AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW);
            builder.append(status, style);
        } else {
            builder.append(String.format("%3d%%", percent), AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
        }


        builder.append(" | Total: ");
        builder.append(String.format("%3d%%", overallPercent),
                AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
        builder.append(String.format(" [%d/%d]", currentMod, totalMods));

        String output = builder.toAnsi(terminal);
        terminal.writer().print(output);

        int padding = terminal.getWidth() - output.length();
        if (padding > 0) {
            terminal.writer().print(" ".repeat(padding));
            terminal.writer().print("\r" + output);
        }

        terminal.writer().flush();
    }

    private void clearLine(Terminal terminal) {
        terminal.writer().print("\r");
        for (int i = 0; i < terminal.getWidth(); i++) {
            terminal.writer().print(" ");
        }
        terminal.writer().print("\r");
        terminal.writer().flush();
    }


    private static class ProgressTrackingInputStream extends FilterInputStream {
        private final ProgressCallback callback;
        private final long totalSize;
        private long bytesRead = 0;
        private long lastCallback = 0;

        public ProgressTrackingInputStream(InputStream in, long totalSize, ProgressCallback callback) {
            super(in);
            this.totalSize = totalSize;
            this.callback = callback;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                updateProgress(1);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytesRead = super.read(b, off, len);
            if (bytesRead != -1) {
                updateProgress(bytesRead);
            }
            return bytesRead;
        }

        private void updateProgress(int bytes) {
            bytesRead += bytes;
            if (bytesRead - lastCallback > totalSize / 50) {
                lastCallback = bytesRead;
                callback.update(bytesRead, totalSize);
            }
        }

        public interface ProgressCallback {
            void update(long bytesRead, long totalSize);
        }
    }
}
