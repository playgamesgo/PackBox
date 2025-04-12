package me.playgamesgo.packbox.utils.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jline.consoleui.elements.ListChoice;
import org.jline.consoleui.elements.PageSizeType;
import org.jline.consoleui.elements.PromptableElementIF;
import org.jline.consoleui.elements.items.ListItemIF;
import org.jline.consoleui.elements.items.impl.ListItem;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public final class QuiltLoader {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(FabricLoader.GameVersion.class, new FabricLoader.GameVersion())
            .registerTypeAdapter(FabricLoader.LoaderVersion.class, new FabricLoader.LoaderVersion())
            .create();

    private static List<FabricLoader.GameVersion> getMinecraftVersions() {
        HttpClient client = HttpClient.newHttpClient();
        String url = "https://meta.quiltmc.org/v3/versions/game";
        String response = client.sendAsync(
                HttpRequest.newBuilder().uri(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofString()
        ).join().body();
        client.close();
        return List.of(gson.fromJson(response, FabricLoader.GameVersion[].class));
    }

    private static List<FabricLoader.LoaderVersion> getLoaderVersion() {
        HttpClient client = HttpClient.newHttpClient();
        String url = "https://meta.quiltmc.org/v3/versions/loader";
        String response = client.sendAsync(
                HttpRequest.newBuilder().uri(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofString()
        ).join().body();
        client.close();
        return List.of(gson.fromJson(response, FabricLoader.LoaderVersion[].class));
    }

    public static PromptableElementIF getMinecraftVersionPrompt() {
        List<FabricLoader.GameVersion> versions = getMinecraftVersions();
        return new ListChoice(
                "Select Minecraft version:", "minecraftVersion", 10, PageSizeType.ABSOLUTE,
                versions.stream()
                        .map(version -> new ListItem(version.getVersion()))
                        .map(item -> (ListItemIF) item).toList()
        );
    }

    public static PromptableElementIF getLoaderVersionPrompt() {
        List<FabricLoader.LoaderVersion> versions = getLoaderVersion();
        return new ListChoice(
                "Select loader version:", "loaderVersion", 10, PageSizeType.ABSOLUTE,
                versions.stream()
                        .map(version -> new ListItem(version.getVersion()))
                        .map(item -> (ListItemIF) item).toList()
        );
    }
}