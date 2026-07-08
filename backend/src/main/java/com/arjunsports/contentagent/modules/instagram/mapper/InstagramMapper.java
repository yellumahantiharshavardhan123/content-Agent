package com.arjunsports.contentagent.modules.instagram.mapper;

import com.arjunsports.contentagent.modules.instagram.dto.InstagramAccountResponse;
import com.arjunsports.contentagent.modules.instagram.dto.InstagramHistoryResponse;
import com.arjunsports.contentagent.modules.instagram.dto.InstagramPostResponse;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramAccount;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPost;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPublishHistory;
import org.springframework.stereotype.Component;

@Component
public class InstagramMapper {

    public InstagramAccountResponse toResponse(InstagramAccount account) {
        return InstagramAccountResponse.builder()
                .id(account.getId())
                .connected(account.isActive())
                .businessAccountId(account.getBusinessAccountId())
                .facebookPageId(account.getFacebookPageId())
                .username(account.getUsername())
                .connectedAt(account.getConnectedAt())
                .disconnectedAt(account.getDisconnectedAt())
                .build();
    }

    public InstagramPostResponse toResponse(InstagramPost post) {
        return InstagramPostResponse.builder()
                .id(post.getId())
                .approvalId(post.getApprovalId())
                .instagramAccountId(post.getInstagramAccountId())
                .mediaId(post.getMediaId())
                .caption(post.getCaption())
                .hashtags(post.getHashtags())
                .status(post.getStatus())
                .instagramMediaId(post.getInstagramMediaId())
                .permalink(post.getPermalink())
                .publisherName(post.getPublisherName())
                .errorMessage(post.getErrorMessage())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .createdBy(post.getCreatedBy())
                .build();
    }

    public InstagramHistoryResponse toResponse(InstagramPublishHistory history) {
        return InstagramHistoryResponse.builder()
                .id(history.getId())
                .instagramPostId(history.getInstagramPostId())
                .action(history.getAction())
                .status(history.getStatus())
                .publisherName(history.getPublisherName())
                .errorMessage(history.getErrorMessage())
                .actorId(history.getActorId())
                .actorEmail(history.getActorEmail())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
