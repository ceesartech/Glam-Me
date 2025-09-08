package tech.ceesar.glamme.social.service;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import tech.ceesar.glamme.common.dto.PagedResponse;
import tech.ceesar.glamme.common.exception.BadRequestException;
import tech.ceesar.glamme.social.dto.CreatePostRequest;
import tech.ceesar.glamme.social.dto.CreatePostResponse;
import tech.ceesar.glamme.social.dto.PostResponse;
import tech.ceesar.glamme.social.entity.Block;
import tech.ceesar.glamme.social.entity.Follow;
import tech.ceesar.glamme.social.entity.Like;
import tech.ceesar.glamme.social.entity.Post;
import tech.ceesar.glamme.social.repositories.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SocialServiceTest {
    @Mock
    PostRepository postRepo;
    @Mock MediaRepository mediaRepo;
    @Mock
    PostTagRepository tagRepo;
    @Mock
    LikeRepository likeRepo;
    @Mock
    CommentRepository commentRepo;
    @Mock
    FollowRepository followRepo;
    @Mock BlockRepository blockRepo;
    @Mock S3Client s3;
    @InjectMocks SocialService service;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        // stub bucket name
        ReflectionTestUtils.setField(service, "bucket", "bucket");
    }

    @Test
    void createPost_uploadsMediaAndTags() {
        UUID userId = UUID.randomUUID();
        CreatePostRequest req = new CreatePostRequest();
        req.setCaption("Hello");
        req.setStylistIds(List.of(UUID.randomUUID()));

        Post savedPost = new Post();
        savedPost.setPostId(UUID.randomUUID());
        when(postRepo.save(any())).thenReturn(savedPost);
        when(tagRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mediaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        // stub S3 putObject
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        MockMultipartFile file = new MockMultipartFile(
                "files", "pic.jpg", "image/jpeg", "data".getBytes());
        CreatePostResponse resp = service.createPost(
                userId, req, List.of(file));

        assertEquals(savedPost.getPostId(), resp.getPostId());
        verify(s3).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(tagRepo).save(any());
    }

    @Test
    void likePost_andUnlike() {
        UUID uid = UUID.randomUUID(), pid = UUID.randomUUID();
        when(likeRepo.findByUserIdAndPostId(uid, pid)).thenReturn(Optional.empty());
        service.likePost(uid, pid);
        verify(likeRepo).save(any());

        // unlike
        Like l = new Like(); l.setLikeId(UUID.randomUUID());
        when(likeRepo.findByUserIdAndPostId(uid, pid)).thenReturn(Optional.of(l));
        service.unlikePost(uid, pid);
        verify(likeRepo).delete(l);
    }

    @Test
    void followAndUnfollow() {
        UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
        when(followRepo.existsByFollowerIdAndFollowedId(u1,u2)).thenReturn(false);
        service.follow(u1,u2);
        verify(followRepo).save(any());

        Follow f = new Follow(); f.setFollowId(UUID.randomUUID());
        when(followRepo.findByFollowerIdAndFollowedId(u1,u2))
                .thenReturn(Optional.of(f));
        service.unfollow(u1,u2);
        verify(followRepo).delete(f);
    }

    @Test
    void blockAndUnblock() {
        UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
        when(blockRepo.existsByBlockerIdAndBlockedId(u1,u2)).thenReturn(false);
        service.block(u1,u2);
        verify(blockRepo).save(any());

        Block b = new Block(); b.setBlockId(UUID.randomUUID());
        when(blockRepo.findByBlockerIdAndBlockedId(u1,u2))
                .thenReturn(Optional.of(b));
        service.unblock(u1,u2);
        verify(blockRepo).delete(b);
    }

    @Test
    void repost_createsNewPost() {
        UUID u1=UUID.randomUUID(), orig=UUID.randomUUID();
        Post old = new Post(); old.setPostId(orig);
        when(postRepo.findById(orig)).thenReturn(Optional.of(old));
        Post newp = new Post(); newp.setPostId(UUID.randomUUID());
        when(postRepo.save(any())).thenReturn(newp);

        CreatePostResponse r = service.repost(u1, orig);
        assertEquals(newp.getPostId(), r.getPostId());
    }

    @Test
    void getFeed_returnsPaged() {
        UUID u1 = UUID.randomUUID(), u2=UUID.randomUUID();
        when(followRepo.findByFollowerId(u1))
                .thenReturn(List.of(new Follow(null,u1,u2,null)));
        Post p = new Post(); p.setPostId(UUID.randomUUID()); p.setUserId(u2);
        Page<Post> page = new PageImpl<>(List.of(p),
                PageRequest.of(0,1), 1);
        when(postRepo.findByUserIdIn(List.of(u2,u1), PageRequest.of(0,1,Sort.by("createdAt").descending())))
                .thenReturn(page);
        when(blockRepo.existsByBlockerIdAndBlockedId(any(),any())).thenReturn(false);
        when(mediaRepo.findAllByPostId(p.getPostId())).thenReturn(List.of());
        when(tagRepo.findByPostId(p.getPostId())).thenReturn(List.of());
        when(likeRepo.countByPostId(p.getPostId())).thenReturn(0L);
        when(commentRepo.countByPostId(p.getPostId())).thenReturn(0L);
        when(postRepo.countByOriginalPostId(p.getPostId())).thenReturn(0L);

        PagedResponse<PostResponse> feed = service.getFeed(u1,0,1);
        assertEquals(1, feed.getContent().size());
    }

    @Test
    void createPost_withoutMedia_Success() {
        UUID userId = UUID.randomUUID();
        CreatePostRequest req = new CreatePostRequest();
        req.setCaption("Text-only post");
        req.setStylistIds(List.of());

        Post savedPost = new Post();
        savedPost.setPostId(UUID.randomUUID());
        savedPost.setUserId(userId);
        savedPost.setCaption("Text-only post");
        when(postRepo.save(any())).thenReturn(savedPost);

        CreatePostResponse resp = service.createPost(userId, req, List.of());

        assertEquals(savedPost.getPostId(), resp.getPostId());
        verify(postRepo).save(any());
        // Should not upload any media
        verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void likePost_alreadyLiked_DoesNothing() {
        UUID uid = UUID.randomUUID(), pid = UUID.randomUUID();
        Like existingLike = new Like(); 
        existingLike.setLikeId(UUID.randomUUID());
        
        when(likeRepo.findByUserIdAndPostId(uid, pid)).thenReturn(Optional.of(existingLike));
        
        service.likePost(uid, pid);
        
        // Should not save a new like
        verify(likeRepo, never()).save(any());
    }

    @Test
    void followUser_alreadyFollowing_ThrowsException() {
        UUID follower = UUID.randomUUID(), followed = UUID.randomUUID();
        when(followRepo.existsByFollowerIdAndFollowedId(follower, followed)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.follow(follower, followed));

        // Should not save a new follow relationship
        verify(followRepo, never()).save(any());
    }

    @Test
    void blockUser_alreadyBlocked_ThrowsException() {
        UUID blocker = UUID.randomUUID(), blocked = UUID.randomUUID();
        when(blockRepo.existsByBlockerIdAndBlockedId(blocker, blocked)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.block(blocker, blocked));

        // Should not save a new block relationship
        verify(blockRepo, never()).save(any());
    }

    @Test
    void getFeed_withBlockedUsers_FiltersOut() {
        UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID(), u3 = UUID.randomUUID();
        
        // User follows both u2 and u3
        when(followRepo.findByFollowerId(u1))
                .thenReturn(List.of(
                    new Follow(null, u1, u2, null),
                    new Follow(null, u1, u3, null)
                ));
        
        Post p2 = new Post(); p2.setPostId(UUID.randomUUID()); p2.setUserId(u2);
        Post p3 = new Post(); p3.setPostId(UUID.randomUUID()); p3.setUserId(u3);
        
        Page<Post> page = new PageImpl<>(List.of(p2, p3), PageRequest.of(0, 10), 2);
        when(postRepo.findByUserIdIn(any(), any())).thenReturn(page);
        
        // u1 has blocked u3
        when(blockRepo.existsByBlockerIdAndBlockedId(u1, u2)).thenReturn(false);
        when(blockRepo.existsByBlockerIdAndBlockedId(u1, u3)).thenReturn(true);
        
        // Mock other dependencies for p2 only (p3 should be filtered out)
        when(mediaRepo.findAllByPostId(p2.getPostId())).thenReturn(List.of());
        when(tagRepo.findByPostId(p2.getPostId())).thenReturn(List.of());
        when(likeRepo.countByPostId(p2.getPostId())).thenReturn(5L);
        when(commentRepo.countByPostId(p2.getPostId())).thenReturn(2L);
        when(postRepo.countByOriginalPostId(p2.getPostId())).thenReturn(1L);

        PagedResponse<PostResponse> feed = service.getFeed(u1, 0, 10);
        
        // Should only contain post from u2 (u3 is blocked)
        assertEquals(1, feed.getContent().size());
    }

    @Test
    void repost_postNotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID nonExistentPostId = UUID.randomUUID();
        
        when(postRepo.findById(nonExistentPostId)).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> 
            service.repost(userId, nonExistentPostId));
        
        verify(postRepo, never()).save(any());
    }
}

