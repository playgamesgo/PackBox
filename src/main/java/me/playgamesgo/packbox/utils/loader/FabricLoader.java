package me.playgamesgo.packbox.utils.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jline.consoleui.elements.ListChoice;
import org.jline.consoleui.elements.PageSizeType;
import org.jline.consoleui.elements.PromptableElementIF;
import org.jline.consoleui.elements.items.ListItemIF;
import org.jline.consoleui.elements.items.impl.ListItem;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public final class FabricLoader {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(GameVersion.class, new GameVersion())
            .registerTypeAdapter(LoaderVersion.class, new LoaderVersion())
            .create();

    private static List<GameVersion> getMinecraftVersions() {
        HttpClient client = HttpClient.newHttpClient();
        String url = "https://meta.fabricmc.net/v2/versions/game";
        String response = client.sendAsync(
                HttpRequest.newBuilder().uri(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofString()
        ).join().body();
        client.close();
        return List.of(gson.fromJson(response, GameVersion[].class));
    }

    private static List<LoaderVersion> getLoaderVersion() {
        HttpClient client = HttpClient.newHttpClient();
        String url = "https://meta.fabricmc.net/v2/versions/loader";
        String response = client.sendAsync(
                HttpRequest.newBuilder().uri(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofString()
        ).join().body();
        client.close();
        return List.of(gson.fromJson(response, LoaderVersion[].class));
    }

    public static PromptableElementIF getMinecraftVersionPrompt() {
        List<GameVersion> versions = getMinecraftVersions();
        return new ListChoice(
                "Select Minecraft version:", "minecraftVersion", 10, PageSizeType.ABSOLUTE,
                versions.stream()
                        .map(version -> new ListItem(version.version))
                        .map(item -> (ListItemIF) item).toList()
        );
    }

    public static PromptableElementIF getLoaderVersionPrompt() {
        List<LoaderVersion> versions = getLoaderVersion();
        return new ListChoice(
                "Select Fabric version:", "loaderVersion", 10, PageSizeType.ABSOLUTE,
                versions.stream()
                        .map(version -> new ListItem(version.version))
                        .map(item -> (ListItemIF) item).toList()
        );
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameVersion extends TypeAdapter<GameVersion> {
        private String version;
        private boolean stable;

        @Override
        public void write(JsonWriter out, GameVersion value) throws IOException {
            out.beginObject();
            out.name("version").value(value.getVersion());
            out.name("stable").value(value.isStable());
            out.endObject();
        }

        @Override
        public GameVersion read(JsonReader in) throws IOException {
            GameVersion version = new GameVersion();
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                if ("version".equals(name)) {
                    version.setVersion(in.nextString());
                } else if ("stable".equals(name)) {
                    version.setStable(in.nextBoolean());
                } else {
                    in.skipValue();
                }
            }
            in.endObject();
            return version;
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoaderVersion extends TypeAdapter<LoaderVersion> {
        private String separator;
        private String build;
        private String maven;
        private String version;
        private boolean stable;

        @Override
        public void write(JsonWriter out, LoaderVersion value) throws IOException {
            out.beginObject();
            out.name("separator").value(value.getSeparator());
            out.name("build").value(value.getBuild());
            out.name("maven").value(value.getMaven());
            out.name("version").value(value.getVersion());
            out.name("stable").value(value.isStable());
            out.endObject();
        }

        @Override
        public LoaderVersion read(JsonReader in) throws IOException {
            LoaderVersion version = new LoaderVersion();
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                switch (name) {
                    case "separator" -> version.setSeparator(in.nextString());
                    case "build" -> version.setBuild(in.nextString());
                    case "maven" -> version.setMaven(in.nextString());
                    case "version" -> version.setVersion(in.nextString());
                    case "stable" -> version.setStable(in.nextBoolean());
                    default -> in.skipValue();
                }
            }
            in.endObject();
            return version;
        }
    }
}
