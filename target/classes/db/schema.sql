-- ==========================================
-- 1. BẢNG PROJECTS
-- ==========================================
CREATE TABLE projects (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    disaster_name NVARCHAR(255) NOT NULL,
    keywords NVARCHAR(MAX) NOT NULL,
    hashtags NVARCHAR(MAX) NOT NULL,
    start_date DATETIME,
    end_date DATETIME,
    platforms NVARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    last_modified DATETIME DEFAULT GETDATE()
);

-- ==========================================
-- 2. BẢNG POSTS
-- ==========================================
CREATE TABLE posts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    project_id BIGINT NOT NULL,
    platform_id NVARCHAR(255) NOT NULL,
    platform NVARCHAR(50) NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    author NVARCHAR(255),
    published_at DATETIME NOT NULL,
    url NVARCHAR(500),
    preprocessed_content NVARCHAR(MAX),
    sentiment NVARCHAR(20),
    damage_categories NVARCHAR(255),
    collected_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT UQ_posts_project_platform UNIQUE (project_id, platform_id),
    CONSTRAINT FK_posts_projects FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Tạo Index cho bảng posts
CREATE INDEX idx_posts_project_id ON posts(project_id);
CREATE INDEX idx_posts_published_at ON posts(published_at);
CREATE INDEX idx_posts_platform_id ON posts(platform_id);
CREATE INDEX idx_posts_sentiment ON posts(sentiment);


-- ==========================================
-- 3. BẢNG COMMENTS
-- ==========================================
CREATE TABLE comments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    post_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    platform_id NVARCHAR(255) NOT NULL,
    platform NVARCHAR(50) NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    author NVARCHAR(255),
    published_at DATETIME NOT NULL,
    preprocessed_content NVARCHAR(MAX),
    sentiment NVARCHAR(20),
    damage_categories NVARCHAR(255),
    collected_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT UQ_comments_project_post_platform UNIQUE (project_id, post_id, platform_id),
    CONSTRAINT FK_comments_posts FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    
    -- Chú ý: Bỏ ON DELETE CASCADE ở đây để tránh lỗi Multiple Cascade Paths của SQL Server. 
    -- Vì khi xóa Project -> Tự động xóa Post -> Sẽ tự động xóa Comment rồi.
    CONSTRAINT FK_comments_projects FOREIGN KEY (project_id) REFERENCES projects(id)
);

-- Tạo Index cho bảng comments
CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_project_id ON comments(project_id);
CREATE INDEX idx_comments_published_at ON comments(published_at);
CREATE INDEX idx_comments_platform_id ON comments(platform_id);
CREATE INDEX idx_comments_sentiment ON comments(sentiment);


-- ==========================================
-- 4. BẢNG AI_SUMMARIES
-- ==========================================
CREATE TABLE ai_summaries (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    project_id BIGINT NOT NULL UNIQUE,
    summary_text NVARCHAR(MAX) NOT NULL,
    generated_at DATETIME DEFAULT GETDATE(),
    posts_analyzed INT NOT NULL,
    comments_analyzed INT NOT NULL,
    model NVARCHAR(50) NOT NULL,
    
    CONSTRAINT FK_ai_summaries_projects FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Tạo Index cho bảng ai_summaries
CREATE INDEX idx_ai_summaries_project_id ON ai_summaries(project_id);