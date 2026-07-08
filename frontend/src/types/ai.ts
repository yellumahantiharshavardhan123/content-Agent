export type ContentType =
  | "INSTAGRAM_CAPTION"
  | "FACEBOOK_CAPTION"
  | "LINKEDIN_POST"
  | "WEBSITE_BLOG"
  | "SEO_TITLE"
  | "SEO_DESCRIPTION"
  | "META_DESCRIPTION"
  | "KEYWORDS"
  | "HASHTAGS"
  | "CALL_TO_ACTION"
  | "REEL_SCRIPT"
  | "SHORT_VIDEO_SCRIPT"
  | "CAROUSEL_CONTENT"
  | "ACHIEVEMENT_ANNOUNCEMENT"
  | "EVENT_ANNOUNCEMENT"
  | "MOTIVATIONAL_POST"
  | "TRAINING_TIP";

export type GenerationStatus = "SUCCESS" | "FAILED";
export type GenerationAction = "GENERATE" | "REGENERATE";

export interface GeneratedContent {
  id: string;
  mediaId: string | null;
  contentType: ContentType;
  promptTemplateId: string | null;
  generatedText: string | null;
  aiModel: string | null;
  status: GenerationStatus;
  errorMessage: string | null;
  draft: boolean;
  edited: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
}

export interface GenerationHistoryEntry {
  id: string;
  generatedContentId: string | null;
  mediaId: string | null;
  contentType: ContentType;
  action: GenerationAction;
  aiModel: string | null;
  status: GenerationStatus;
  errorMessage: string | null;
  latencyMs: number | null;
  actorEmail: string | null;
  createdAt: string;
}

export interface PromptTemplate {
  id: string;
  contentType: ContentType;
  name: string;
  description: string | null;
  systemPrompt: string;
  userPromptTemplate: string;
  version: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface GenerateContentInput {
  mediaId?: string | null;
  contentType: ContentType;
  manualNotes?: string;
  eventDetails?: string;
  achievement?: string;
  competitionResults?: string;
  trainingSession?: string;
  coachNotes?: string;
}
