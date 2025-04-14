package me.playgamesgo.packbox.utils.source;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.playgamesgo.packbox.utils.Manifest;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class Modrinth {
    public static @Nullable Manifest.Mod getMod(File file) {
        @Nullable String sha1 = createSha1(file);
        if (sha1 == null) {
            log.error("Failed to create SHA-1 hash for file: {}", file.getAbsolutePath());
            return null;
        }

        HttpClient client = HttpClient.newHttpClient();
        String url = "https://api.modrinth.com/v2/version_file/" + sha1;
        HttpResponse<String> response = client.sendAsync(
                HttpRequest.newBuilder().uri(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofString()
        ).join();
        client.close();
        if (response == null || response.body().isEmpty() || response.statusCode() != 200) return null;
        Response modrinthResponse = Response.gson.fromJson(response.body(), Response.class);
        Manifest.Mod mod = new Manifest.Mod();
        mod.setPath(file.getPath());
        mod.setUrl(modrinthResponse.getFiles().getFirst().getUrl());
        mod.setSource(Manifest.Source.MODRINTH);
        mod.setSha1(sha1);
        return mod;
    }

    public static @Nullable String createSha1(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            FileInputStream fis = new FileInputStream(file);
            byte[] byteArray = new byte[1024];
            int bytesCount;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();

            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Error creating SHA-1 hash", e);
            return null;
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response extends TypeAdapter<Response> {
        public static final Gson gson = new GsonBuilder()
                .registerTypeAdapter(Response.class, new Response())
                .create();

        private List<Files> files = new ArrayList<>();

        @Override
        public void write(JsonWriter out, Response value) throws IOException {
            out.beginObject();
            out.name("files");
            out.beginArray();
            for (Files file : value.getFiles()) {
                out.beginObject();
                out.name("url").value(file.getUrl());
                out.endObject();
            }
            out.endArray();
            out.endObject();
        }

        @Override
        public Response read(JsonReader in) throws IOException {
            Response response = new Response();
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                if (name.equals("files")) {
                    in.beginArray();
                    while (in.hasNext()) {
                        Files file = new Files();
                        in.beginObject();
                        while (in.hasNext()) {
                            String fieldName = in.nextName();
                            if (fieldName.equals("url")) {
                                file.setUrl(in.nextString());
                            } else {
                                in.skipValue();
                            }
                        }
                        in.endObject();
                        response.getFiles().add(file);
                    }
                    in.endArray();
                } else {
                    in.skipValue();
                }
            }
            in.endObject();
            return response;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Files {
            private String url;
        }
    }
}
