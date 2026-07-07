import { Sparkles } from "lucide-react";
import { ModulePlaceholder } from "@/components/shared/module-placeholder";

export default function ContentGeneratorPage() {
  return (
    <ModulePlaceholder
      icon={Sparkles}
      title="AI Content Generator"
      description="Generate Instagram captions, hashtags, blog articles, SEO metadata, and reel scripts from uploaded media."
      moduleNumber={4}
    />
  );
}
