"use client";

import { useCallback, useEffect, useState } from "react";
import { GenerationForm } from "@/components/ai/generation-form";
import { GeneratedContentList } from "@/components/ai/generated-content-list";
import { listContent } from "@/lib/api/ai";
import type { GeneratedContent } from "@/types/ai";

export default function ContentGeneratorPage() {
  const [items, setItems] = useState<GeneratedContent[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await listContent(0, 48);
      setItems(response.data?.content ?? []);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    refresh();
  }, [refresh]);

  function handleGenerated(content: GeneratedContent) {
    setItems((prev) => [content, ...prev]);
  }

  function handleChanged(updated: GeneratedContent) {
    setItems((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
  }

  function handleDeleted(id: string) {
    setItems((prev) => prev.filter((item) => item.id !== id));
  }

  return (
    <div className="flex flex-1 flex-col gap-6 p-6">
      <div>
        <h2 className="text-2xl font-semibold tracking-tight">AI Content Generator</h2>
        <p className="text-muted-foreground">
          Generate Instagram/Facebook/LinkedIn posts, blog articles, SEO metadata, scripts, and more from uploaded
          media or free-text context.
        </p>
      </div>

      <GenerationForm onGenerated={handleGenerated} />

      <div>
        <h3 className="mb-4 text-lg font-medium">Generated content</h3>
        <GeneratedContentList
          items={items}
          isLoading={isLoading}
          onChanged={handleChanged}
          onDeleted={handleDeleted}
        />
      </div>
    </div>
  );
}
