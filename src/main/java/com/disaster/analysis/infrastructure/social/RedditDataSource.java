package com.disaster.analysis.infrastructure.social;

import com.disaster.analysis.domain.exception.DataSourceException;
import com.disaster.analysis.domain.model.Comment;
import com.disaster.analysis.domain.model.Post;
import com.disaster.analysis.domain.model.enums.Platform;
import com.disaster.analysis.domain.contract.social.DataSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class RedditDataSource implements DataSource {

    private static final String BASE_URL = "https://www.reddit.com";
    private static final int PAGE_SIZE = 25;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<Post> fetchPosts(String query, LocalDateTime startDate, LocalDateTime endDate, int maxResults) throws DataSourceException {
        List<Post> results = new ArrayList<>();
        try {
            String url = BASE_URL + "/search.json?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&sort=new&limit=" + maxResults;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", "disaster-app").GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) throw new DataSourceException(getPlatform(), "Lỗi mạng Reddit");

            JsonNode children = mapper.readTree(response.body()).path("data").path("children");
            for (JsonNode child : children) {
                JsonNode data = child.path("data");
                Post post = new Post();
                post.setPlatform(getPlatform().name());
                post.setPlatformId("reddit_" + data.path("id").asText());
                post.setAuthor(data.path("author").asText());
                post.setContent(data.path("title").asText() + "\n" + data.path("selftext").asText());
                post.setUrl(BASE_URL + data.path("permalink").asText());
                post.setPublishedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(data.path("created_utc").asLong()), ZoneId.systemDefault()));
                post.setCollectedAt(LocalDateTime.now());
                results.add(post);
            }
            return results;
        } catch (Exception e) {
            throw new DataSourceException(getPlatform(), "Lỗi Reddit: " + e.getMessage());
        }
    }

    @Override
    public List<Comment> fetchComments(Post post, int maxResults) throws DataSourceException {
        return new ArrayList<>(); // Rút gọn để đồng bộ kiến trúc
    }

    @Override
    public Platform getPlatform() {
        return Platform.REDDIT;
    }
}