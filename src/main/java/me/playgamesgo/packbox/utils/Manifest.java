package me.playgamesgo.packbox.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public final class Manifest extends TypeAdapter<Manifest> {
    public static Gson gson = new GsonBuilder()
            .registerTypeAdapter(Manifest.class, new Manifest())
            .create();

    private final String formatVersion = "1.0";
    @NotNull private String name;
    @NotNull private String author;
    @NotNull private String version;
    @NotNull private Loader loader;
    @NotNull private String minecraftVersion;
    @NotNull private String loaderVersion;
    @NotNull private Source source;
    @NotNull private Source fallbackSource;
    private List<Mod> mods = new ArrayList<>();

    public enum Loader {
        FABRIC, FORGE, NEOFORGE, QUILT
    }

    public enum Source {
        MODRINTH, CURSEFORGE, NONE
    }

    @Override
    public void write(JsonWriter out, Manifest value) throws IOException {
        out.beginObject();

        out.name("formatVersion").value(value.getFormatVersion());
        out.name("name").value(value.getName());
        out.name("author").value(value.getAuthor());
        out.name("version").value(value.getVersion());
        out.name("loader").value(value.getLoader().name());
        out.name("minecraftVersion").value(value.getMinecraftVersion());
        out.name("loaderVersion").value(value.getLoaderVersion());
        out.name("source").value(value.getSource().name());
        out.name("fallbackSource").value(value.getFallbackSource().name());

        out.name("mods");
        out.beginArray();
        for (Mod mod : value.getMods()) {
            out.beginObject();
            out.name("path").value(mod.getPath());
            out.name("url").value(mod.getUrl());
            out.name("source").value(mod.getSource().name());
            out.name("sha1").value(mod.getSha1());
            out.endObject();
        }
        out.endArray();

        out.endObject();
    }

    @Override
    public Manifest read(JsonReader in) throws IOException {
        Manifest manifest = new Manifest();
        in.beginObject();

        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "name" -> manifest.setName(in.nextString());
                case "author" -> manifest.setAuthor(in.nextString());
                case "version" -> manifest.setVersion(in.nextString());
                case "loader" -> manifest.setLoader(Loader.valueOf(in.nextString().toUpperCase()));
                case "minecraftVersion" -> manifest.setMinecraftVersion(in.nextString());
                case "loaderVersion" -> manifest.setLoaderVersion(in.nextString());
                case "source" -> manifest.setSource(Source.valueOf(in.nextString().toUpperCase()));
                case "fallbackSource" -> manifest.setFallbackSource(Source.valueOf(in.nextString().toUpperCase()));
                case "mods" -> {
                    in.beginArray();
                    while (in.hasNext()) {
                        Mod mod = new Mod();
                        in.beginObject();
                        while (in.hasNext()) {
                            String modName = in.nextName();
                            switch (modName) {
                                case "path" -> mod.setPath(in.nextString());
                                case "url" -> mod.setUrl(in.nextString());
                                case "source" -> mod.setSource(Source.valueOf(in.nextString().toUpperCase()));
                                case "sha1" -> mod.setSha1(in.nextString());
                                default -> in.skipValue();
                            }
                        }
                        manifest.getMods().add(mod);
                        in.endObject();
                    }
                    in.endArray();
                }
                default -> in.skipValue();
            }
        }

        in.endObject();
        return manifest;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Mod {
        private String path;
        private String url;
        private Manifest.Source source;
        private String sha1;
    }
}
