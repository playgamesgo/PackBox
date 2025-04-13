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
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class CurseForge {
    public static @Nullable Manifest.Mod getMod(File file) {
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            log.error("Failed to read file: {}", file.getAbsolutePath(), e);
            return null;
        }
        long fingerprint = Integer.toUnsignedLong(computeFingerprint(fileBytes));

        FingerprintRequest request = new FingerprintRequest();
        request.getFingerprints().add(fingerprint);
        String requestBody = FingerprintRequest.gson.toJson(request);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.curseforge.com/v1/fingerprints"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("x-api-key", "$2a$10$bL4bIL5pUWqfcO7KQtnMReakwtfHbNKh6v1uTpKlzhwoueEJQnPnm")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("Failed to send request to CurseForge API: {}", e.getMessage());
            return null;
        }
        client.close();

        if (response == null || response.body().isEmpty() || response.statusCode() != 200) return null;
        CurseForgeResponse curseForgeResponse = CurseForgeResponse.gson.fromJson(response.body(), CurseForgeResponse.class);
        if (curseForgeResponse.getData().getExactMatches().isEmpty()) return null;
        Manifest.Mod mod = new Manifest.Mod();
        mod.setPath(file.getPath());
        mod.setUrl(curseForgeResponse.getData().getExactMatches().getFirst().getFile().getDownloadUrl());
        mod.setSource(Manifest.Source.CURSEFORGE);
        return mod;
    }

    private static int computeFingerprint(byte[] buffer) {
        final int mask = 0xFFFFFFFF;
        final int multiplex = 1540483477;

        int num1 = computeNormalizedLength(buffer);
        int num2 = 1 ^ num1;
        int num3 = 0;
        int num4 = 0;

        for (byte b : buffer) {
            if (!isWhitespaceCharacter(b)) {
                num3 |= ((b & 0xFF) << num4) & mask;
                num4 += 8;
                if (num4 == 32) {
                    int num6 = (num3 * multiplex) & mask;
                    int num7 = ((num6 ^ (num6 >>> 24)) * multiplex) & mask;

                    num2 = ((num2 * multiplex) ^ num7) & mask;
                    num3 = 0;
                    num4 = 0;
                }
            }
        }

        if (num4 > 0) {
            num2 = ((num2 ^ num3) * multiplex) & mask;
        }

        int num6 = ((num2 ^ (num2 >>> 13)) * multiplex) & mask;

        return num6 ^ (num6 >>> 15);
    }

    private static int computeNormalizedLength(byte[] buffer) {
        int count = 0;
        for (byte b : buffer) {
            if (!isWhitespaceCharacter(b)) {
                ++count;
            }
        }
        return count;
    }

    private static boolean isWhitespaceCharacter(byte b) {
        return b == 9 || b == 10 || b == 13 || b == 32;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class FingerprintRequest extends TypeAdapter<FingerprintRequest> {
        public static Gson gson = new GsonBuilder()
                .registerTypeAdapter(FingerprintRequest.class, new FingerprintRequest())
                .create();

        private List<Long> fingerprints = new ArrayList<>();

        @Override
        public void write(JsonWriter out, FingerprintRequest value) throws IOException {
            out.beginObject();
            out.name("fingerprints");
            out.beginArray();
            for (Long fingerprint : value.getFingerprints()) {
                out.value(fingerprint);
            }
            out.endArray();
            out.endObject();
        }

        @Override
        public FingerprintRequest read(JsonReader in) throws IOException {
            FingerprintRequest request = new FingerprintRequest();
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                if ("fingerprints".equals(name)) {
                    in.beginArray();
                    while (in.hasNext()) {
                        request.getFingerprints().add(in.nextLong());
                    }
                    in.endArray();
                } else {
                    in.skipValue();
                }
            }
            in.endObject();
            return request;
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CurseForgeResponse extends TypeAdapter<CurseForgeResponse> {
        public static Gson gson = new GsonBuilder()
                .registerTypeAdapter(CurseForgeResponse.class, new CurseForgeResponse())
                .create();

        private CurseForgeData data;

        @Override
        public void write(JsonWriter out, CurseForgeResponse value) throws IOException {
            out.beginObject();
            out.name("data");
            out.beginObject();

            out.name("exactMatches");
            out.beginArray();
            for (ExactMatch match : value.getData().getExactMatches()) {
                out.beginObject();
                out.name("file");
                out.beginObject();
                out.name("downloadUrl").value(match.getFile().getDownloadUrl());
                out.endObject();
                out.endObject();
            }
            out.endArray();

            out.endObject();
            out.endObject();
        }

        @Override
        public CurseForgeResponse read(JsonReader in) throws IOException {
            CurseForgeResponse response = new CurseForgeResponse();
            response.setData(new CurseForgeData());

            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                if ("data".equals(name)) {
                    in.beginObject();
                    while (in.hasNext()) {
                        String dataField = in.nextName();
                        if ("exactMatches".equals(dataField)) {
                            List<ExactMatch> exactMatches = new ArrayList<>();
                            in.beginArray();
                            while (in.hasNext()) {
                                ExactMatch match = new ExactMatch();
                                in.beginObject();
                                while (in.hasNext()) {
                                    String matchField = in.nextName();
                                    if ("file".equals(matchField)) {
                                        in.beginObject();
                                        File file = new File();
                                        while (in.hasNext()) {
                                            String fileField = in.nextName();
                                            if ("downloadUrl".equals(fileField)) {
                                                file.setDownloadUrl(in.nextString());
                                            } else {
                                                in.skipValue();
                                            }
                                        }
                                        in.endObject();
                                        match.setFile(file);
                                    } else {
                                        in.skipValue();
                                    }
                                }
                                in.endObject();
                                exactMatches.add(match);
                            }
                            in.endArray();
                            response.getData().setExactMatches(exactMatches);
                        } else {
                            in.skipValue();
                        }
                    }
                    in.endObject();
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
        public static class CurseForgeData {
            private List<ExactMatch> exactMatches = new ArrayList<>();
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class ExactMatch {
            private File file;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class File {
            private String downloadUrl;
        }
    }
}
