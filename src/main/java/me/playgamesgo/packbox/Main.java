package me.playgamesgo.packbox;

import me.playgamesgo.packbox.commands.InitCommand;
import picocli.CommandLine;

@CommandLine.Command(
        name = "packbox",
        mixinStandardHelpOptions = true,
        version = "packbox 1.0",
        description = "A CLI tool for development Minecraft modpacks and managing them with git.",
        subcommands = {
                InitCommand.class,

        })
public final class Main {
        public static void main(String[] args) {
                System.exit(new CommandLine(new Main()).execute("init"));
        }
}