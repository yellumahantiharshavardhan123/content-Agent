package com.arjunsports.contentagent.modules.instagram.service;

import com.arjunsports.contentagent.modules.instagram.dto.ConnectAccountRequest;
import com.arjunsports.contentagent.modules.instagram.dto.InstagramAccountResponse;
import com.arjunsports.contentagent.modules.instagram.dto.InstagramHistoryResponse;
import com.arjunsports.contentagent.modules.instagram.dto.InstagramPostResponse;
import com.arjunsports.contentagent.modules.instagram.dto.PublishRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InstagramService {

    InstagramAccountResponse connect(ConnectAccountRequest request);

    InstagramAccountResponse getStatus();

    InstagramPostResponse publish(PublishRequest request);

    InstagramPostResponse publishMock(PublishRequest request);

    Page<InstagramHistoryResponse> getHistory(Pageable pageable);

    void disconnect();
}
