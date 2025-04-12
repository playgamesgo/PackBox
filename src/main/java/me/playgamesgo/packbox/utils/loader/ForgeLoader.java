package me.playgamesgo.packbox.utils.loader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.util.Map;

public final class ForgeLoader {
    private static final Gson gson = new Gson();

    private static Map<String, List<String>> getVersionsMap() {
        HttpClient client = HttpClient.newHttpClient();
        String url = "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json";
        String response = client.sendAsync(
                HttpRequest.newBuilder().uri(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofString()
        ).join().body();
        client.close();

        return gson.fromJson(response, new TypeToken<Map<String, List<String>>>(){}.getType());
    }

    public static PromptableElementIF getMinecraftVersionPrompt() {
        Map<String, List<String>> versions = getVersionsMap();

        return new ListChoice(
                "Select Minecraft version:", "minecraftVersion", 10, PageSizeType.ABSOLUTE,
                versions.keySet().stream()
                        .map(ListItem::new)
                        .map(item -> (ListItemIF) item)
                        .toList().reversed()
        );
    }

    public static PromptableElementIF getLoaderVersionPrompt(String minecraftVersion) {
        Map<String, List<String>> versionsMap = getVersionsMap();
        List<String> loaderVersions = versionsMap.get(minecraftVersion);

        return new ListChoice(
                "Select Forge version:", "loaderVersion", 10, PageSizeType.ABSOLUTE,
                loaderVersions.stream()
                        .map(ListItem::new)
                        .map(item -> (ListItemIF) item)
                        .toList().reversed()
        );
    }
}
