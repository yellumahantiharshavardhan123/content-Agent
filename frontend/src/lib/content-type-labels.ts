import type { ContentType } from "@/types/ai";

export const CONTENT_TYPE_LABELS: Record<ContentType, string> = {
  INSTAGRAM_CAPTION: "Instagram Caption",
  FACEBOOK_CAPTION: "Facebook Caption",
  LINKEDIN_POST: "LinkedIn Post",
  WEBSITE_BLOG: "Website Blog",
  SEO_TITLE: "SEO Title",
  SEO_DESCRIPTION: "SEO Description",
  META_DESCRIPTION: "Meta Description",
  KEYWORDS: "Keywords",
  HASHTAGS: "Hashtags",
  CALL_TO_ACTION: "Call To Action",
  REEL_SCRIPT: "Reel Script",
  SHORT_VIDEO_SCRIPT: "Short Video Script",
  CAROUSEL_CONTENT: "Carousel Content",
  ACHIEVEMENT_ANNOUNCEMENT: "Achievement Announcement",
  EVENT_ANNOUNCEMENT: "Event Announcement",
  MOTIVATIONAL_POST: "Motivational Post",
  TRAINING_TIP: "Training Tip",
};

export const CONTENT_TYPES = Object.keys(CONTENT_TYPE_LABELS) as ContentType[];
