package com.disaster.analysis.infrastructure.social;

import com.disaster.analysis.domain.contract.social.DataSource;
import com.disaster.analysis.domain.exception.DataSourceException;
import com.disaster.analysis.domain.model.Comment;
import com.disaster.analysis.domain.model.Post;
import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.model.enums.Platform;
import com.disaster.analysis.domain.model.enums.Sentiment;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bộ sinh dữ liệu giả lập (Mock).
 * Giúp Test hệ thống khi không có mạng.
 */
public class MockDataSource implements DataSource {

    private final Platform platform;
    private final Random random = new Random();

    public MockDataSource(Platform platform) {
        this.platform = platform;
    }

    @Override
    public List<Post> fetchPosts(String query, LocalDateTime startDate, LocalDateTime endDate, int maxResults) throws DataSourceException {
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < maxResults; i++) {
            Post post = new Post();
            post.setPlatform(platform.name());
            post.setPlatformId(platform.name() + "_" + UUID.randomUUID().toString().substring(0, 8));
            post.setAuthor("Người dùng giả lập " + i);
            post.setContent("Cập nhật tình hình mưa bão " + query + " gây ngập lụt diện rộng.");
            post.setUrl("https://mock.com/" + i);
            post.setPublishedAt(LocalDateTime.now().minusDays(random.nextInt(5)));

            // Ép kiểu tập hợp Enum thành chuỗi String lưu Database
            Set<DamageCategory> cats = Set.of(DamageCategory.BUILDING_DAMAGE, DamageCategory.INFRASTRUCTURE_DAMAGE);
            post.setDamageCategories(cats.stream().map(Enum::name).collect(Collectors.joining(",")));

            post.setCollectedAt(LocalDateTime.now());
            posts.add(post);
        }
        return posts;
    }

    @Override
    public List<Comment> fetchComments(Post post, int maxResults) throws DataSourceException {
        List<Comment> comments = new ArrayList<>();
        for(int i = 0; i < maxResults; i++){
            Comment c = new Comment();
            c.setPlatform(platform.name());
            c.setPlatformId(platform.name() + "_cmt_" + i);
            c.setAuthor("Người bình luận " + i);
            c.setContent("Cầu mong mọi người bình an!");
            c.setPostId(post.getId());
            c.setPublishedAt(LocalDateTime.now());
            c.setCollectedAt(LocalDateTime.now());
            comments.add(c);
        }
        return comments;
    }

    @Override
    public Platform getPlatform() {
        return platform;
    }
}