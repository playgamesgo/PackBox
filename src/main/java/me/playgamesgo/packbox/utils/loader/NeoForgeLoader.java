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
import java.util.ArrayList;
import java.util.List;

public final class NeoForgeLoader {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(NeoForgeResponse.class, new NeoForgeResponse())
            .create();

    private static List<String> getVersions() {
        HttpClient client = HttpClient.newHttpClient();
        String url = "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge";
        String response = client.sendAsync(
                HttpRequest.newBuilder().uri(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofString()
        ).join().body();
        client.close();
        return gson.fromJson(response, NeoForgeResponse.class).versions;
    }

    public static PromptableElementIF getMinecraftVersionPrompt() {
        List<String> versions = getVersions();
        return new ListChoice(
                "Select Minecraft version:", "minecraftVersion", 10, PageSizeType.ABSOLUTE,
                versions.stream().filter(s -> s.startsWith("2"))
                        .map(s -> "1." + s.substring(0, 4)).distinct()
                        .map(ListItem::new).map(item -> (ListItemIF) item).toList()
        );
    }

    public static PromptableElementIF getLoaderVersionPrompt(String minecraftVersion) {
        List<String> versions = getVersions();
        return new ListChoice(
                "Select NeoForge version:", "loaderVersion", 10, PageSizeType.ABSOLUTE,
                versions.stream().filter(s -> s.startsWith(minecraftVersion.substring(2, 6)))
                        .map(ListItem::new)
                        .map(item -> (ListItemIF) item).toList()
        );
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NeoForgeResponse extends TypeAdapter<NeoForgeResponse> {
        private boolean isSnapshot;
        private List<String> versions;

        @Override
        public void write(JsonWriter out, NeoForgeResponse value) throws IOException {
            out.beginObject();
            out.name("isSnapshot").value(value.isSnapshot);
            out.name("versions").beginArray();
            for (String version : value.versions) {
                out.value(version);
            }
            out.endArray();
            out.endObject();
        }

        @Override
        public NeoForgeResponse read(JsonReader in) throws IOException {
            NeoForgeResponse response = new NeoForgeResponse();
            response.versions = new ArrayList<>();
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                if (name.equals("isSnapshot")) {
                    response.isSnapshot = in.nextBoolean();
                } else if (name.equals("versions")) {
                    in.beginArray();
                    while (in.hasNext()) {
                        response.versions.add(in.nextString());
                    }
                    in.endArray();
                } else {
                    in.skipValue();
                }
            }
            in.endObject();
            return response;
        }
    }
}
