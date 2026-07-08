package com.arjunsports.contentagent.modules.instagram;

import com.arjunsports.contentagent.common.util.CredentialEncryptionUtil;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramAccount;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramAccountInfo;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramPublisherProperties;
import com.arjunsports.contentagent.modules.instagram.repository.InstagramAccountRepository;
import com.arjunsports.contentagent.modules.instagram.provider.PublisherResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * If IG_BUSINESS_ACCOUNT_ID and IG_ACCESS_TOKEN are both set and no account is
 * connected yet, auto-connects on startup - the same convenience {@code
 * AdminBootstrapRunner} provides for the first admin user (Module 1), applied
 * here so the long-scaffolded (but previously unused) static Instagram env
 * vars actually do something. Manual connection via POST /api/instagram/connect
 * remains the primary path for admins who don't set these.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramAccountBootstrapRunner implements ApplicationRunner {

    private final InstagramAccountRepository accountRepository;
    private final PublisherResolver publisherResolver;
    private final CredentialEncryptionUtil credentialEncryptionUtil;
    private final InstagramPublisherProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accountRepository.findByActiveTrue().isPresent()) {
            return;
        }

        String businessAccountId = properties.getBusinessAccountId();
        String accessToken = properties.getAccessToken();
        if (businessAccountId == null || businessAccountId.isBlank() || accessToken == null || accessToken.isBlank()) {
            return;
        }

        try {
            InstagramAccountInfo info = publisherResolver.resolve().verifyAccount(businessAccountId, accessToken);
            InstagramAccount account = InstagramAccount.builder()
                    .businessAccountId(info.businessAccountId())
                    .username(info.username())
                    .accessTokenEncrypted(credentialEncryptionUtil.encrypt(accessToken))
                    .active(true)
                    .connectedAt(Instant.now())
                    .build();
            accountRepository.save(account);
            log.info("Bootstrapped Instagram connection from IG_BUSINESS_ACCOUNT_ID/IG_ACCESS_TOKEN for account {}",
                    info.businessAccountId());
        } catch (Exception e) {
            log.warn("IG_BUSINESS_ACCOUNT_ID/IG_ACCESS_TOKEN are set but could not be verified - skipping "
                    + "auto-connect. Connect manually via POST /api/instagram/connect instead. Cause: {}",
                    e.getMessage());
        }
    }
}
