package tech.ceesar.glamme.social.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tech.ceesar.glamme.common.dto.PagedResponse;
import tech.ceesar.glamme.common.exception.BadRequestException;
import tech.ceesar.glamme.common.exception.ResourceNotFoundException;
import tech.ceesar.glamme.social.dto.*;
import tech.ceesar.glamme.social.entity.*;
import tech.ceesar.glamme.social.repositories.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocialService {
    private final PostRepository postRepo;
    private final MediaRepository mediaRepo;
    private final PostTagRepository tagRepo;
    private final LikeRepository likeRepo;
    private final CommentRepository commentRepo;
    private final FollowRepository followRepo;
    private final BlockRepository blockRepo;

    private final S3Client s3;
    @Value("${aws.s3.bucket}")
    private String bucket;

    private String buildUrl(String key) {
        return String.format("https://%s.s3.amazonaws.com/%s",
                bucket, URLEncoder.encode(key, StandardCharsets.UTF_8));
    }

    @Transactional
    public CreatePostResponse createPost(
            UUID userId,
            CreatePostRequest req,
            List<MultipartFile> files
    ) {
        Post post = Post.builder()
                .userId(userId)
                .caption(req.getCaption())
                .build();
        post = postRepo.save(post);

        // handle tags
        for (UUID sty : req.getStylistIds()) {
            tagRepo.save(PostTag.builder()
                    .post(post)
                    .stylistId(sty)
                    .build());
        }

        // handle media upload
        for (MultipartFile file : files) {
            String ext = Optional.ofNullable(file.getOriginalFilename())
                    .filter(n -> n.contains("."))
                    .map(n -> n.substring(n.lastIndexOf('.'))).orElse("");
            String key = "posts/" + post.getPostId() + "/" + UUID.randomUUID() + ext;
            try {
                s3.putObject(PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(file.getContentType())
                                .build(),
                        RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            } catch (IOException e) {
                throw new BadRequestException("Failed to upload media");
            }
            MediaType type = file.getContentType().startsWith("video")
                    ? MediaType.VIDEO : MediaType.IMAGE;
            mediaRepo.save(Media.builder()
                    .post(post)
                    .mediaUrl(buildUrl(key))
                    .mediaType(type)
                    .build());
        }

        return new CreatePostResponse(post.getPostId());
    }

    public PostResponse getPost(UUID userId, UUID postId) {
        Post p = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId.toString()));
        // check block
        if (blockRepo.existsByBlockerIdAndBlockedId(p.getUserId(), userId)
                || blockRepo.existsByBlockerIdAndBlockedId(userId, p.getUserId())) {
            throw new BadRequestException("Post not accessible");
        }
        List<MediaDto> medias = mediaRepo.findAllByPostId(postId).stream()
                .map(m -> MediaDto.builder()
                        .id(m.getMediaId())
                        .url(m.getMediaUrl())
                        .type(m.getMediaType().name())
                        .build())
                .toList();
        List<UUID> tags = tagRepo.findByPostId(postId).stream()
                .map(PostTag::getStylistId).toList();
        long likeCount = likeRepo.countByPostId(postId);
        long commentCount = commentRepo.countByPostId(postId);
        long repostCount = postRepo.countByOriginalPostId(postId);
        return PostResponse.builder()
                .id(p.getPostId())
                .userId(p.getUserId())
                .caption(p.getCaption())
                .createdAt(p.getCreatedAt())
                .media(medias)
                .tags(tags)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .repostCount(repostCount)
                .originalPostId(Optional.ofNullable(p.getOriginalPost()).map(Post::getPostId).orElse(null))
                .build();
    }

    public PagedResponse<PostResponse> getFeed(UUID userId, int page, int size) {
        // 1) Build a mutable list of followees + self
        List<UUID> followees = followRepo.findByFollowerId(userId).stream()
                .map(Follow::getFollowedId)
                .collect(Collectors.toCollection(ArrayList::new));
        followees.add(userId);

        // 2) Page through posts by those users
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> postsPage = postRepo.findByUserIdIn(followees, pageable);

        // 3) Map each Post entity *directly* to PostResponse, filtering out blocked posts
        List<PostResponse> dtos = postsPage.getContent().stream()
            .filter(p -> {
                // Block-check: skip posts from/to blocked users
                return !blockRepo.existsByBlockerIdAndBlockedId(p.getUserId(), userId) &&
                       !blockRepo.existsByBlockerIdAndBlockedId(userId, p.getUserId());
            })
            .map(p -> {

            // Media
            List<MediaDto> mediaDtos = mediaRepo.findAllByPostId(p.getPostId()).stream()
                    .map(m -> MediaDto.builder()
                            .id(m.getMediaId())
                            .url(m.getMediaUrl())
                            .type(m.getMediaType().name())
                            .build())
                    .toList();

            // Tags
            List<UUID> tagIds = tagRepo.findByPostId(p.getPostId()).stream()
                    .map(PostTag::getStylistId)
                    .toList();

            // Counts
            long likeCount    = likeRepo.countByPostId(p.getPostId());
            long commentCount = commentRepo.countByPostId(p.getPostId());
            long repostCount  = postRepo.countByOriginalPostId(p.getPostId());

            // Original
            UUID originalId = Optional.ofNullable(p.getOriginalPost())
                    .map(Post::getPostId)
                    .orElse(null);

            // Build
            return PostResponse.builder()
                    .id(p.getPostId())
                    .userId(p.getUserId())
                    .caption(p.getCaption())
                    .createdAt(p.getCreatedAt())
                    .media(mediaDtos)
                    .tags(tagIds)
                    .likeCount(likeCount)
                    .commentCount(commentCount)
                    .repostCount(repostCount)
                    .originalPostId(originalId)
                    .build();
        }).toList();

        // 4) Return paged response
        return PagedResponse.of(
                dtos,
                postsPage.getNumber(),
                postsPage.getSize(),
                postsPage.getTotalElements()
        );
    }

    @Transactional
    public void deletePost(UUID userId, UUID postId, boolean isAdmin) {
        Post p = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId.toString()));
        if (!isAdmin && !p.getUserId().equals(userId)) {
            throw new BadRequestException("Not authorized");
        }
        postRepo.delete(p);
    }

    @Transactional
    public void likePost(UUID userId, UUID postId) {
        if (likeRepo.findByUserIdAndPostId(userId, postId).isPresent()) return;
        likeRepo.save(Like.builder()
                .userId(userId)
                .postId(postId)
                .build());
    }

    @Transactional
    public void unlikePost(UUID userId, UUID postId) {
        likeRepo.findByUserIdAndPostId(userId, postId)
                .ifPresent(likeRepo::delete);
    }

    @Transactional
    public CommentResponse addComment(UUID userId, UUID postId, CommentRequest req) {
        Post p = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId.toString()));
        Comment c = commentRepo.save(Comment.builder()
                .userId(userId)
                .post(p)
                .content(req.getContent())
                .build());
        return new CommentResponse(c.getCommentId(), c.getUserId(), c.getContent(), c.getCreatedAt());
    }

    public List<CommentResponse> getComments(UUID postId) {
        return commentRepo.findByPostId(postId).stream()
                .map(c -> new CommentResponse(c.getCommentId(), c.getUserId(), c.getContent(), c.getCreatedAt()))
                .toList();
    }

    @Transactional
    public FollowResponse follow(UUID follower, UUID followee) {
        if (followRepo.existsByFollowerIdAndFollowedId(follower, followee)) {
            throw new BadRequestException("Already following");
        }
        // build entity
        Follow toSave = Follow.builder()
                .followerId(follower)
                .followedId(followee)
                .build();
        // persist, but ignore returned value so tests don't NPE
        followRepo.save(toSave);
        return new FollowResponse(follower, followee);
    }

    @Transactional
    public void unfollow(UUID follower, UUID followee) {
        followRepo.findByFollowerIdAndFollowedId(follower, followee)
                .ifPresent(followRepo::delete);
    }

    @Transactional
    public BlockResponse block(UUID blocker, UUID blocked) {
        if (blockRepo.existsByBlockerIdAndBlockedId(blocker, blocked)) {
            throw new BadRequestException("Already blocked");
        }
        Block toSave = Block.builder()
                .blockerId(blocker)
                .blockedId(blocked)
                .build();
        blockRepo.save(toSave);
        return new BlockResponse(blocker, blocked);
    }

    @Transactional
    public void unblock(UUID blocker, UUID blocked) {
        blockRepo.findByBlockerIdAndBlockedId(blocker, blocked)
                .ifPresent(blockRepo::delete);
    }

    @Transactional
    public CreatePostResponse repost(UUID userId, UUID postId) {
        Post original = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId.toString()));
        Post rp = Post.builder()
                .userId(userId)
                .caption(original.getCaption())
                .originalPost(original)
                .build();
        rp = postRepo.save(rp);
        return new CreatePostResponse(rp.getPostId());
    }
}

