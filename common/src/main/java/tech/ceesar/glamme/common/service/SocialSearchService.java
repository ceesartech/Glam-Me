package tech.ceesar.glamme.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialSearchService {

    private final OpenSearchService openSearchService;

    private static final String POSTS_INDEX = "posts";
    private static final String HASHTAGS_INDEX = "hashtags";
    private static final String USERS_INDEX = "users";

    /**
     * Initialize social search indices
     */
    public void initializeIndices() {
        initializePostsIndex();
        initializeHashtagsIndex();
        initializeUsersIndex();
    }

    private boolean initializePostsIndex() {
        Map<String, Object> mapping = new HashMap<>();

        Map<String, Object> textField = Map.of("type", "text", "analyzer", "standard");
        Map<String, Object> keywordField = Map.of("type", "keyword");

        mapping.put("content", textField);
        mapping.put("author_id", keywordField);
        mapping.put("author_username", keywordField);
        mapping.put("hashtags", Map.of("type", "keyword"));
        mapping.put("mentions", Map.of("type", "keyword"));
        mapping.put("media_urls", Map.of("type", "keyword"));
        mapping.put("location", Map.of(
                "type", "geo_point",
                "ignore_malformed", true
        ));
        mapping.put("created_at", Map.of("type", "date"));
        mapping.put("likes_count", Map.of("type", "integer"));
        mapping.put("comments_count", Map.of("type", "integer"));
        mapping.put("shares_count", Map.of("type", "integer"));
        mapping.put("is_featured", Map.of("type", "boolean"));
        mapping.put("visibility", keywordField);

        return openSearchService.createIndex(POSTS_INDEX, mapping);
    }

    private boolean initializeHashtagsIndex() {
        Map<String, Object> mapping = new HashMap<>();

        mapping.put("tag", Map.of("type", "keyword"));
        mapping.put("post_ids", Map.of("type", "keyword"));
        mapping.put("usage_count", Map.of("type", "integer"));
        mapping.put("trending_score", Map.of("type", "float"));
        mapping.put("last_used", Map.of("type", "date"));
        mapping.put("category", Map.of("type", "keyword"));

        return openSearchService.createIndex(HASHTAGS_INDEX, mapping);
    }

    private boolean initializeUsersIndex() {
        Map<String, Object> mapping = new HashMap<>();

        Map<String, Object> textField = Map.of("type", "text", "analyzer", "standard");
        Map<String, Object> keywordField = Map.of("type", "keyword");

        mapping.put("username", keywordField);
        mapping.put("display_name", textField);
        mapping.put("bio", textField);
        mapping.put("location", textField);
        mapping.put("website", keywordField);
        mapping.put("verified", Map.of("type", "boolean"));
        mapping.put("follower_count", Map.of("type", "integer"));
        mapping.put("following_count", Map.of("type", "integer"));
        mapping.put("post_count", Map.of("type", "integer"));
        mapping.put("joined_at", Map.of("type", "date"));
        mapping.put("last_active", Map.of("type", "date"));

        return openSearchService.createIndex(USERS_INDEX, mapping);
    }

    /**
     * Index a post
     */
    public boolean indexPost(PostDocument post) {
        return openSearchService.indexDocument(POSTS_INDEX, post.getId(), post).isPresent();
    }

    /**
     * Search posts by content
     */
    public List<OpenSearchService.SearchResult<PostDocument>> searchPosts(String query, int from, int size) {
        return openSearchService.searchByText(POSTS_INDEX, "content", query, PostDocument.class, from, size);
    }

    /**
     * Search posts by hashtags
     */
    public List<OpenSearchService.SearchResult<PostDocument>> searchPostsByHashtag(String hashtag, int from, int size) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("hashtags", hashtag);
        return openSearchService.searchWithFilters(POSTS_INDEX, "content", "*", filters, PostDocument.class, from, size);
    }

    /**
     * Search posts by user
     */
    public List<OpenSearchService.SearchResult<PostDocument>> searchPostsByUser(String username, int from, int size) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("author_username", username);
        return openSearchService.searchWithFilters(POSTS_INDEX, "content", "*", filters, PostDocument.class, from, size);
    }

    /**
     * Get trending hashtags
     */
    public List<OpenSearchService.SearchResult<HashtagDocument>> getTrendingHashtags(int limit) {
        // This would require sorting by trending_score - simplified implementation
        return openSearchService.searchByText(HASHTAGS_INDEX, "tag", "*", HashtagDocument.class, 0, limit);
    }

    /**
     * Search users
     */
    public List<OpenSearchService.SearchResult<UserDocument>> searchUsers(String query, int from, int size) {
        return openSearchService.searchMultiMatch(USERS_INDEX,
                List.of("username", "display_name", "bio"), query, UserDocument.class, from, size);
    }

    /**
     * Get post suggestions for search
     */
    public List<String> getPostSearchSuggestions(String prefix, int size) {
        return openSearchService.getSuggestions(POSTS_INDEX, "content", prefix, size);
    }

    /**
     * Get user suggestions for mentions
     */
    public List<String> getUserMentions(String prefix, int size) {
        return openSearchService.getSuggestions(USERS_INDEX, "username", prefix, size);
    }

    /**
     * Get hashtag suggestions
     */
    public List<String> getHashtagSuggestions(String prefix, int size) {
        return openSearchService.getSuggestions(HASHTAGS_INDEX, "tag", prefix, size);
    }

    /**
     * Update post engagement metrics
     */
    public boolean updatePostMetrics(String postId, int likesCount, int commentsCount, int sharesCount) {
        // This would require a partial update - simplified implementation
        PostDocument existingPost = getPost(postId);
        if (existingPost != null) {
            existingPost.setLikesCount(likesCount);
            existingPost.setCommentsCount(commentsCount);
            existingPost.setSharesCount(sharesCount);
            return openSearchService.updateDocument(POSTS_INDEX, postId, existingPost);
        }
        return false;
    }

    /**
     * Get post by ID
     */
    public PostDocument getPost(String postId) {
        return openSearchService.getDocument(POSTS_INDEX, postId, PostDocument.class).orElse(null);
    }

    /**
     * Delete post
     */
    public boolean deletePost(String postId) {
        return openSearchService.deleteDocument(POSTS_INDEX, postId);
    }

    /**
     * Bulk index posts
     */
    public int bulkIndexPosts(Map<String, PostDocument> posts) {
        return openSearchService.bulkIndex(POSTS_INDEX, posts);
    }

    /**
     * Bulk index users
     */
    public int bulkIndexUsers(Map<String, UserDocument> users) {
        return openSearchService.bulkIndex(USERS_INDEX, users);
    }

    /**
     * Get total post count
     */
    public long getTotalPostCount() {
        return openSearchService.getDocumentCount(POSTS_INDEX);
    }

    /**
     * Get total user count
     */
    public long getTotalUserCount() {
        return openSearchService.getDocumentCount(USERS_INDEX);
    }

    /**
     * Refresh all indices
     */
    public void refreshAllIndices() {
        openSearchService.refreshIndex(POSTS_INDEX);
        openSearchService.refreshIndex(HASHTAGS_INDEX);
        openSearchService.refreshIndex(USERS_INDEX);
    }

    // Document classes for social search

    public static class PostDocument {
        private String id;
        private String content;
        private String authorId;
        private String authorUsername;
        private List<String> hashtags;
        private List<String> mentions;
        private List<String> mediaUrls;
        private double latitude;
        private double longitude;
        private String createdAt;
        private int likesCount;
        private int commentsCount;
        private int sharesCount;
        private boolean isFeatured;
        private String visibility;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getAuthorId() { return authorId; }
        public void setAuthorId(String authorId) { this.authorId = authorId; }

        public String getAuthorUsername() { return authorUsername; }
        public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

        public List<String> getHashtags() { return hashtags; }
        public void setHashtags(List<String> hashtags) { this.hashtags = hashtags; }

        public List<String> getMentions() { return mentions; }
        public void setMentions(List<String> mentions) { this.mentions = mentions; }

        public List<String> getMediaUrls() { return mediaUrls; }
        public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public int getLikesCount() { return likesCount; }
        public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

        public int getCommentsCount() { return commentsCount; }
        public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

        public int getSharesCount() { return sharesCount; }
        public void setSharesCount(int sharesCount) { this.sharesCount = sharesCount; }

        public boolean isFeatured() { return isFeatured; }
        public void setFeatured(boolean isFeatured) { this.isFeatured = isFeatured; }

        public String getVisibility() { return visibility; }
        public void setVisibility(String visibility) { this.visibility = visibility; }
    }

    public static class HashtagDocument {
        private String tag;
        private List<String> postIds;
        private int usageCount;
        private float trendingScore;
        private String lastUsed;
        private String category;

        // Getters and setters
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }

        public List<String> getPostIds() { return postIds; }
        public void setPostIds(List<String> postIds) { this.postIds = postIds; }

        public int getUsageCount() { return usageCount; }
        public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

        public float getTrendingScore() { return trendingScore; }
        public void setTrendingScore(float trendingScore) { this.trendingScore = trendingScore; }

        public String getLastUsed() { return lastUsed; }
        public void setLastUsed(String lastUsed) { this.lastUsed = lastUsed; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    public static class UserDocument {
        private String id;
        private String username;
        private String displayName;
        private String bio;
        private String location;
        private String website;
        private boolean verified;
        private int followerCount;
        private int followingCount;
        private int postCount;
        private String joinedAt;
        private String lastActive;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getWebsite() { return website; }
        public void setWebsite(String website) { this.website = website; }

        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }

        public int getFollowerCount() { return followerCount; }
        public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }

        public int getFollowingCount() { return followingCount; }
        public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

        public int getPostCount() { return postCount; }
        public void setPostCount(int postCount) { this.postCount = postCount; }

        public String getJoinedAt() { return joinedAt; }
        public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }

        public String getLastActive() { return lastActive; }
        public void setLastActive(String lastActive) { this.lastActive = lastActive; }
    }
}
