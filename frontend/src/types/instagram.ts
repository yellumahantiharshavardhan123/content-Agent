export type InstagramPostStatus = "PENDING" | "PUBLISHING" | "PUBLISHED" | "FAILED";

export type InstagramHistoryAction =
  | "CONNECT"
  | "DISCONNECT"
  | "PUBLISH_ATTEMPT"
  | "PUBLISH_SUCCESS"
  | "PUBLISH_FAILURE"
  | "RETRY";

export interface InstagramAccount {
  id: string | null;
  connected: boolean;
  businessAccountId: string | null;
  facebookPageId: string | null;
  username: string | null;
  connectedAt: string | null;
  disconnectedAt: string | null;
}

export interface InstagramPost {
  id: string;
  approvalId: string | null;
  instagramAccountId: string | null;
  mediaId: string | null;
  caption: string;
  hashtags: string | null;
  status: InstagramPostStatus;
  instagramMediaId: string | null;
  permalink: string | null;
  publisherName: string | null;
  errorMessage: string | null;
  publishedAt: string | null;
  createdAt: string;
  createdBy: string | null;
}

export interface InstagramHistoryEntry {
  id: string;
  instagramPostId: string | null;
  action: InstagramHistoryAction;
  status: InstagramPostStatus | null;
  publisherName: string | null;
  errorMessage: string | null;
  actorId: string | null;
  actorEmail: string | null;
  createdAt: string;
}

export interface ConnectAccountInput {
  businessAccountId: string;
  facebookPageId?: string;
  accessToken: string;
}

export interface PublishInput {
  approvalId: string;
  mediaId: string;
  caption: string;
  hashtags?: string;
}
