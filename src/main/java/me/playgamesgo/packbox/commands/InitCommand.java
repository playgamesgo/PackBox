package me.playgamesgo.packbox.commands;

import lombok.extern.slf4j.Slf4j;
import me.playgamesgo.packbox.utils.Manifest;
import me.playgamesgo.packbox.utils.loader.FabricLoader;
import me.playgamesgo.packbox.utils.loader.ForgeLoader;
import me.playgamesgo.packbox.utils.loader.NeoForgeLoader;
import me.playgamesgo.packbox.utils.loader.QuiltLoader;
import org.jetbrains.annotations.Nullable;
import org.jline.consoleui.elements.*;
import org.jline.consoleui.elements.items.impl.ListItem;
import org.jline.consoleui.prompt.ConsolePrompt;
import org.jline.consoleui.prompt.PromptResultItemIF;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(
        name = "init",
        description = "Initialize a new modpack in the current directory.")
public final class InitCommand implements Callable<Integer> {
    @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the modpack")
    @Nullable private String packName;

    @CommandLine.Option(names = {"-a", "--author"}, description = "Author of the modpack")
    @Nullable private String author;

    @CommandLine.Option(names = {"-v", "--version"}, description = "Version of the modpack")
    @Nullable private String modpackVersion;

    @CommandLine.Option(names = {"-l", "--loader"}, description = "Mod loader (fabric, forge, quilt, neoforge)")
    @Nullable private String loader;

    @CommandLine.Option(names = {"-m", "--minecraft"}, description = "Minecraft version")
    @Nullable private String minecraftVersion;

    @CommandLine.Option(names = {"--loader-version"}, description = "Loader version")
    @Nullable private String loaderVersion;

    @CommandLine.Option(names = {"-s", "--source"}, description = "Primary mod source (Modrinth, Curseforge)")
    @Nullable private String source;

    @CommandLine.Option(names = {"-f", "--fallback"}, description = "Fallback mod source (None, or the other source)")
    @Nullable private String fallbackSource;


    @Override
    public Integer call() {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            ConsolePrompt prompt = new ConsolePrompt(terminal);
            List<AttributedString> warns = new ArrayList<>();
            Path manifest = Paths.get("manifest.packbox.json");
            if (Files.exists(manifest)) {
                AttributedStringBuilder builder = new AttributedStringBuilder();
                builder.style(style -> style.bold().foreground(AttributedStyle.YELLOW).bold()).append("Warning: manifest.packbox.json already exists. It will be overwritten.");
                warns.add(builder.toAttributedString());
            }
            Map<String, PromptResultItemIF> result = prompt.prompt(warns, this::promptData);

            if (result.get("packName").getResult() != null) packName = result.get("packName").getResult();
            if (result.get("author").getResult() != null) author = result.get("author").getResult();
            if (result.get("modpackVersion").getResult() != null) modpackVersion = result.get("modpackVersion").getResult();
            if (result.get("loader").getResult() != null) loader = result.get("loader").getResult();
            if (result.get("minecraftVersion").getResult() != null) minecraftVersion = result.get("minecraftVersion").getResult();
            if (result.get("loaderVersion").getResult() != null) loaderVersion = result.get("loaderVersion").getResult();
            if (result.get("source").getResult() != null) source = result.get("source").getResult();
            if (result.get("fallbackSource").getResult() != null) fallbackSource = result.get("fallbackSource").getResult();

            Files.writeString(manifest, Manifest.gson.toJson(new Manifest(packName, author, modpackVersion,
                    Manifest.Loader.valueOf(loader.toUpperCase()), minecraftVersion, loaderVersion,
                    Manifest.Source.valueOf(source.toUpperCase()), Manifest.Source.valueOf(fallbackSource.toUpperCase()))));
        } catch (IOException e) {
            log.error("Error initializing terminal: ", e);
        }

        return 0;
    }

    private List<PromptableElementIF> promptData(Map<String, PromptResultItemIF> results) {
        if (results.get("packName") == null && packName == null) return List.of(
                new InputValue("packName", "Enter modpack name:", null, Paths.get("").toAbsolutePath().getFileName().toString())
        );

        if (results.get("author") == null && author == null) return List.of(
                new InputValue("author", "Enter author name:", null, null)
        );

        if (results.get("modpackVersion") == null && modpackVersion == null) return List.of(
                new InputValue("modpackVersion", "Enter modpack version:", null, null)
        );
        if (results.get("loader") == null && loader == null) return List.of(
                new ListChoice("Select mod loader:", "loader", 10, PageSizeType.ABSOLUTE,
                        List.of(new ListItem("fabric"), new ListItem("forge"), new ListItem("neoforge"), new ListItem("quilt")))
        );


        if (results.get("minecraftVersion") == null && minecraftVersion == null) {
            String loader = results.get("loader").getResult() == null ? this.loader : results.get("loader").getResult();

            switch (loader) {
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
        }

        if (results.get("loaderVersion") == null && loaderVersion == null) {
            String loader = results.get("loader").getResult() == null ? this.loader : results.get("loader").getResult();

            switch (loader) {
                case "forge" -> {
                    return List.of(ForgeLoader.getLoaderVersionPrompt(results.get("minecraftVersion").getResult()));
                }
                case "neoforge" -> {
                    return List.of(NeoForgeLoader.getLoaderVersionPrompt(results.get("minecraftVersion").getResult()));
                }
                default -> throw new IllegalStateException("Unexpected value: " + results.get("loader"));
            }
        }


        if (results.get("source") == null && source == null) return List.of(
                new ListChoice("Select mod source:", "source", 10, PageSizeType.ABSOLUTE,
                        List.of(new ListItem("Modrinth"), new ListItem("Curseforge")))
        );

        if (results.get("fallbackSource") == null && source == null && fallbackSource == null) return List.of(
                new ListChoice("Select fallback mod source:", "fallbackSource", 10, PageSizeType.ABSOLUTE,
                        List.of(new ListItem("None"), results.get("source").getResult().equals("Modrinth") ? new ListItem("Curseforge") : new ListItem("Modrinth")))
        ); else if (results.get("fallbackSource") == null && source != null && fallbackSource == null) {
            fallbackSource = Manifest.Source.NONE.toString();
        }

        return null;
    }
}