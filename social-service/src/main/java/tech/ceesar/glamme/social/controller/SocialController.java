package tech.ceesar.glamme.social.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.ceesar.glamme.common.dto.PagedResponse;
import tech.ceesar.glamme.social.dto.*;
import tech.ceesar.glamme.social.service.SocialService;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialController {
    private final SocialService service;

    // Create post with media & tags
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreatePostResponse> createPost(
            Principal principal,
            @RequestPart("data") CreatePostRequest req,
            @RequestPart("files") List<MultipartFile> files
    ) {
        UUID userId = UUID.fromString(principal.getName());
        return ResponseEntity.status(201)
                .body(service.createPost(userId, req, files));
    }

    // Get single post
    @GetMapping("/posts/{postId}")
    public PostResponse getPost(
            Principal principal,
            @PathVariable UUID postId
    ) {
        UUID userId = UUID.fromString(principal.getName());
        return service.getPost(userId, postId);
    }

    // Feed
    @GetMapping("/feed")
    public PagedResponse<PostResponse> feed(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID userId = UUID.fromString(principal.getName());
        return service.getFeed(userId, page, size);
    }

    // Delete post
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            Principal principal,
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "false") boolean isAdmin
    ) {
        UUID userId = UUID.fromString(principal.getName());
        service.deletePost(userId, postId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    // Like / Unlike
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Void> like(
            Principal principal,
            @PathVariable UUID postId
    ) {
        UUID userId = UUID.fromString(principal.getName());
        service.likePost(userId, postId);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/posts/{postId}/like")
    public ResponseEntity<Void> unlike(
            Principal principal,
            @PathVariable UUID postId
    ) {
        UUID userId = UUID.fromString(principal.getName());
        service.unlikePost(userId, postId);
        return ResponseEntity.noContent().build();
    }

    // Comments
    @PostMapping("/posts/{postId}/comments")
    public CommentResponse comment(
            Principal principal,
            @PathVariable UUID postId,
            @RequestBody CommentRequest req
    ) {
        UUID userId = UUID.fromString(principal.getName());
        return service.addComment(userId, postId, req);
    }
    @GetMapping("/posts/{postId}/comments")
    public List<CommentResponse> comments(@PathVariable UUID postId) {
        return service.getComments(postId);
    }

    // Follow / Unfollow
    @PostMapping("/follow/{followeeId}")
    public FollowResponse follow(
            Principal principal,
            @PathVariable UUID followeeId
    ) {
        UUID userId = UUID.fromString(principal.getName());
        return service.follow(userId, followeeId);
    }
    @DeleteMapping("/follow/{followeeId}")
    public ResponseEntity<Void> unfollow(
            Principal principal,
            @PathVariable UUID followeeId
    ) {
        UUID userId = UUID.fromString(principal.getName());
        service.unfollow(userId, followeeId);
        return ResponseEntity.noContent().build();
    }

    // Block / Unblock
    @PostMapping("/block/{blockedId}")
    public BlockResponse block(
            Principal principal,
            @PathVariable UUID blockedId
    ) {
        UUID userId = UUID.fromString(principal.getName());
        return service.block(userId, blockedId);
    }
    @DeleteMapping("/block/{blockedId}")
    public ResponseEntity<Void> unblock(
            Principal principal,
            @PathVariable UUID blockedId
    ) {
        UUID userId = UUID.fromString(principal.getName());
        service.unblock(userId, blockedId);
        return ResponseEntity.noContent().build();
    }

    // Repost
    @PostMapping("/posts/{postId}/repost")
    public ResponseEntity<CreatePostResponse> repost(
            Principal principal,
            @PathVariable UUID postId
    ) {
        UUID userId = UUID.fromString(principal.getName());
        return ResponseEntity.ok(service.repost(userId, postId));
    }
}
