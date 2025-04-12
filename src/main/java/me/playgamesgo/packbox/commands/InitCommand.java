package me.playgamesgo.packbox.commands;

import lombok.extern.slf4j.Slf4j;
import me.playgamesgo.packbox.utils.loader.FabricLoader;
import me.playgamesgo.packbox.utils.loader.ForgeLoader;
import me.playgamesgo.packbox.utils.loader.NeoForgeLoader;
import me.playgamesgo.packbox.utils.loader.QuiltLoader;
import org.jetbrains.annotations.Nullable;
import org.jline.consoleui.elements.*;
import org.jline.consoleui.elements.items.impl.ListItem;
import org.jline.consoleui.prompt.ConsolePrompt;
import org.jline.consoleui.prompt.PromptResultItemIF;
import org.jline.consoleui.prompt.builder.PromptBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(
        name = "init",
        version = "packbox 1.0",
        description = "Initialize a new modpack in the current directory.")
public final class InitCommand implements Callable<Integer> {
    @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the modpack")
    @Nullable
    private String packName;

    @CommandLine.Option(names = {"-a", "--author"}, description = "Author of the modpack")
    @Nullable
    private String author;

    @CommandLine.Option(names = {"-v", "--version"}, description = "Version of the modpack")
    @Nullable
    private String modpackVersion;

    @CommandLine.Option(names = {"-l", "--loader"}, description = "Mod loader (fabric, forge, quilt, neoforge)")
    @Nullable
    private String loader;

    @CommandLine.Option(names = {"-m", "--minecraft"}, description = "Minecraft version")
    @Nullable
    private String minecraftVersion;

    @CommandLine.Option(names = {"--loader-version"}, description = "Loader version")
    @Nullable
    private String loaderVersion;

    @CommandLine.Option(names = {"-s", "--source"}, description = "Primary mod source (Modrinth, Curseforge)")
    @Nullable
    private String source;

    @CommandLine.Option(names = {"-f", "--fallback"}, description = "Fallback mod source (None, or the other source)")
    @Nullable
    private String fallbackSource;

    private PromptBuilder promptBuilder;

    @Override
    public Integer call() {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            ConsolePrompt prompt = new ConsolePrompt(terminal);
            promptBuilder = prompt.getPromptBuilder();

            promptBuilder.createInputPrompt()
                    .name("packName")
                    .message("Enter modpack name:")
                    .defaultValue(Paths.get("").toAbsolutePath().getFileName().toString())
                    .addPrompt();
            promptBuilder.createInputPrompt()
                    .name("author")
                    .message("Enter author name:")
                    .addPrompt();
            promptBuilder.createInputPrompt()
                    .name("modpackVersion")
                    .message("Enter modpack version:")
                    .addPrompt();

            promptBuilder.createListPrompt()
                    .name("loader")
                    .message("Select mod loader:")
                    .newItem().text("fabric").add()
                    .newItem().text("forge").add()
                    .newItem().text("neoforge").add()
                    .newItem().text("quilt").add()
                    .addPrompt();

            Map<String, PromptResultItemIF> result = prompt.prompt(this::promptAdditionalData);
            packName = result.get("packName").getResult();
            author = result.get("author").getResult();
            modpackVersion = result.get("modpackVersion").getResult();
            loader = result.get("loader").getResult();
            minecraftVersion = result.get("minecraftVersion").getResult();
            loaderVersion = result.get("loaderVersion").getResult();
            source = result.get("source").getResult();
            fallbackSource = result.get("fallbackSource").getResult();
        } catch (IOException e) {
            log.error("Error initializing terminal: ", e);
        }

        return 1;
    }

    private List<PromptableElementIF> promptAdditionalData(Map<String, PromptResultItemIF> results) {
        if (results.get("packName") == null) return promptBuilder.build();


        if (results.get("minecraftVersion") == null) switch (results.get("loader").getResult()) {
            case "fabric" -> {
                return List.of(FabricLoader.getMinecraftVersionPrompt(), FabricLoader.getLoaderVersionPrompt());
            }
            case "forge" -> {
                return List.of(ForgeLoader.getMinecraftVersionPrompt());
            }
            case "neoforge" -> {
                return List.of(NeoForgeLoader.getMinecraftVersionPrompt());
            }
            case "quilt" -> {
                return List.of(QuiltLoader.getMinecraftVersionPrompt(), QuiltLoader.getLoaderVersionPrompt());
            }
            default -> throw new IllegalStateException("Unexpected value: " + results.get("loader"));
        }

        if (results.get("loaderVersion") == null) switch (results.get("loader").getResult()) {
            case "forge" -> {
                return List.of(ForgeLoader.getLoaderVersionPrompt(results.get("minecraftVersion").getResult()));
            }
            case "neoforge" -> {
                return List.of(NeoForgeLoader.getLoaderVersionPrompt(results.get("minecraftVersion").getResult()));
            }
            default -> throw new IllegalStateException("Unexpected value: " + results.get("loader"));
        }


        if (results.get("source") == null) return List.of(
                new ListChoice("Select mod source:", "source", 10, PageSizeType.ABSOLUTE,
                        List.of(new ListItem("Modrinth"), new ListItem("Curseforge")))
        );

        if (results.get("fallbackSource") == null) return List.of(
                new ListChoice("Select fallback mod source:", "fallbackSource", 10, PageSizeType.ABSOLUTE,
                        List.of(new ListItem("None"), results.get("source").getResult().equals("Modrinth") ? new ListItem("Curseforge") : new ListItem("Modrinth")))
        );

        return null;
    }
}