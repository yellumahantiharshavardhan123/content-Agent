package com.arjunsports.contentagent.modules.approval.entity;

/** Every action recorded in the {@link ApprovalHistory} append-only log. */
public enum ApprovalAction {
    SUBMIT,
    APPROVE,
    REJECT,
    COMMENT
}
