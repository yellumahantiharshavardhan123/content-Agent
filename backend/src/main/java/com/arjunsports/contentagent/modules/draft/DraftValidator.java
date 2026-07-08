package com.arjunsports.contentagent.modules.draft;

import com.arjunsports.contentagent.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DraftValidator {

    public void validateContentText(String contentText) {
        if (contentText == null || contentText.isBlank()) {
            throw new BadRequestException("Draft content cannot be empty");
        }
    }

    /** Parses a status query/body value, or returns {@code null} if none was supplied. Rejects unknown values. */
    public DraftStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return DraftStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid status: " + rawStatus + ". Must be one of " + Arrays.toString(DraftStatus.values()));
        }
    }
}
