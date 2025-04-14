package me.playgamesgo.packbox.commands;

import lombok.extern.slf4j.Slf4j;
import me.playgamesgo.packbox.utils.Manifest;
import me.playgamesgo.packbox.utils.source.CurseForge;
import me.playgamesgo.packbox.utils.source.Modrinth;
import org.jetbrains.annotations.Nullable;
import org.jline.consoleui.elements.ConfirmChoice;
import org.jline.consoleui.prompt.ConfirmResult;
import org.jline.consoleui.prompt.ConsolePrompt;
import org.jline.consoleui.prompt.PromptResultItemIF;
import org.jline.consoleui.prompt.builder.PromptBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(
        name = "detect",
        description = "Detect files sources from files"
)
public final class DetectCommand implements Callable<Integer> {
    @CommandLine.Option(names = {"-f", "--folders"},
            description = "Folders to detect mods from, comma separated (mods, resourcepacks, shaderpacks)")
    @Nullable
    private String folders;

    private final List<Manifest.Mod> foundedMods = new ArrayList<>();

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
            ConsolePrompt prompt = new ConsolePrompt(terminal);
            PromptBuilder promptBuilder = prompt.getPromptBuilder();

            Path curseForgeTokenFile = Paths.get(".curseforge");
            if (!Files.exists(curseForgeTokenFile) &&
                    (manifest.getSource().equals(Manifest.Source.CURSEFORGE) || manifest.getFallbackSource().equals(Manifest.Source.CURSEFORGE))) {
                promptBuilder.createConfirmPromp().
                        name("token")
                        .message("CurseForge token not found, You can fetch official launcher's token, but using " +
                                "official CurseForge Launcher's token may break CurseForge's Terms of Service.\n" +
                                "You can specify your own token in the .curseforge file. " +
                                "Do you want to fetch official launcher's token?")
                        .addPrompt();

                Map<String, PromptResultItemIF> result = prompt.prompt(promptBuilder.build());
                if (((ConfirmResult) result.get("token")).getConfirmed().equals(ConfirmChoice.ConfirmationValue.YES)) {
                    Files.writeString(curseForgeTokenFile, "$2a$10$bL4bIL5pUWqfcO7KQtnMReakwtfHbNKh6v1uTpKlzhwoueEJQnPnm");
                    AttributedStringBuilder successMessage = new AttributedStringBuilder()
                            .append("Token fetched successfully", AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
                    System.out.println(successMessage.toAnsi(terminal));
                } else Files.createFile(curseForgeTokenFile);
            }

            if (folders == null) {
                promptBuilder = prompt.getPromptBuilder();
                promptBuilder.createCheckboxPrompt()
                        .name("folders")
                        .message("Select a folder to detect sources")
                        .newItem("mods").text("mods").add()
                        .newItem("resourcepacks").text("resourcepacks").add()
                        .newItem("shaderpacks").text("shaderpacks").add()
                        .addPrompt();

                Map<String, PromptResultItemIF> result = prompt.prompt(promptBuilder.build());
                folders = result.get("folders").getResult().replaceAll("[\\[\\]]", "");
            }


            List<String> unsupportedFolders = Arrays.stream(folders.split(","))
                    .map(String::trim)
                    .filter(folder -> !List.of("mods", "resourcepacks", "shaderpacks").contains(folder))
                    .toList();

            if (!unsupportedFolders.isEmpty()) {
                AttributedStringBuilder message = new AttributedStringBuilder()
                        .append("Unsupported folders detected: ", AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                        .append(String.join(", ", unsupportedFolders), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                System.out.println(message.toAnsi(terminal));
                return 1;
            }


            List<String> selectedFolders = new ArrayList<>();
            if (!folders.isEmpty()) {
                selectedFolders = Arrays.stream(folders.split(",")).map(String::trim).toList();
            }

            if (selectedFolders.isEmpty()) {
                AttributedStringBuilder message = new AttributedStringBuilder().append("No folders selected",
                        AttributedStyle.BOLD.foreground(AttributedStyle.RED));
                System.out.println(message.toAnsi(terminal));
            }

            for (String folder : selectedFolders) {
                AttributedStringBuilder message = new AttributedStringBuilder()
                        .append("Processing folder: ", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                        .append(folder, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
                System.out.println(message.toAnsi(terminal));

                File dir = new File(folder);
                if (!dir.exists() || !dir.isDirectory()) {
                    AttributedStringBuilder errorMessage = new AttributedStringBuilder()
                            .append("Error: ", AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                            .append("Directory does not exist or is not a directory: ", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                            .append(folder, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                    System.out.println(errorMessage.toAnsi(terminal));
                    continue;
                }

                for (File file : dir.listFiles()) {
                    if (file.isFile()) {
                        AttributedStringBuilder fileMessage = new AttributedStringBuilder()
                                .append("Checking file: ", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                                .append(file.getName(), AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
                        System.out.println(fileMessage.toAnsi(terminal));

                        @Nullable Manifest.Mod mod = switch (manifest.getSource()) {
                            case MODRINTH -> Modrinth.getMod(file);
                            case CURSEFORGE -> CurseForge.getMod(file, Files.readString(curseForgeTokenFile));
                            default -> throw new IllegalStateException("Unexpected value: " + manifest.getSource());
                        };

                        if (mod == null) {
                            if (manifest.getFallbackSource().equals(Manifest.Source.NONE)) {
                                AttributedStringBuilder notFoundMessage = new AttributedStringBuilder()
                                        .append("File not found in primary source: ", AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                                        .append(file.getName(), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                                System.out.println(notFoundMessage.toAnsi(terminal));
                                continue;
                            } else {
                                AttributedStringBuilder fallbackMessage = new AttributedStringBuilder()
                                        .append("File not found in primary source, checking fallback source: ", AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW))
                                        .append(manifest.getFallbackSource().name(), AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
                                System.out.println(fallbackMessage.toAnsi(terminal));

                                mod = switch (manifest.getFallbackSource()) {
                                    case MODRINTH -> Modrinth.getMod(file);
                                    case CURSEFORGE -> CurseForge.getMod(file, Files.readString(curseForgeTokenFile));
                                    default -> throw new IllegalStateException("Unexpected value: " + manifest.getFallbackSource());
                                };

                                if (mod == null) {
                                    AttributedStringBuilder notFoundFallbackMessage = new AttributedStringBuilder()
                                            .append("File not found in fallback source: ", AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                                            .append(file.getName(), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                                    System.out.println(notFoundFallbackMessage.toAnsi(terminal));
                                    continue;
                                }
                            }
                        }

                        foundedMods.add(mod);
                        AttributedStringBuilder modMessage = new AttributedStringBuilder()
                                .append("Found files source: ", AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
                                .append(mod.getPath(), AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                                .append(" (", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                                .append(mod.getSource().name(), AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                                .append(")", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
                        System.out.println(modMessage.toAnsi(terminal));
                    }
                }

                manifest.getMods().clear();
                manifest.getMods().addAll(foundedMods);
                Files.writeString(manifestFile, Manifest.gson.toJson(manifest));
            }
        } catch (Exception e) {
            log.error("Error initializing terminal", e);
        }

        return 0;
    }
}
