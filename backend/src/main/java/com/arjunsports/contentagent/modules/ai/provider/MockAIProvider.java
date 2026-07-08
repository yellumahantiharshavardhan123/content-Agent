package com.arjunsports.contentagent.modules.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stand-in for a real AI backend, used so Modules 3 and 4 can be developed and
 * demoed end-to-end without an OpenAI (or other provider) API key. Only ever
 * registered when the {@code dev} profile is active - it does not exist as a
 * bean at all under {@code prod}, so no configuration mistake in production
 * can accidentally select it (see {@link AIProviderResolver}, which can only
 * pick among beans Spring actually created).
 *
 * <p>Which canned response to return is inferred from the resolved system/user
 * prompt text (distinctive phrases lifted from the seeded {@code PromptTemplate}
 * rows - see {@code V5__prompt_template_seed.sql}), since {@link AIGenerationRequest}
 * deliberately carries only plain prompt strings, not a {@code ContentType}, to
 * keep this interface identical to what a real provider sees. If an admin edits
 * a template enough that none of these phrases remain, generation still
 * succeeds with a generic-but-realistic fallback caption - this is a soft
 * heuristic for dev/demo content, not a contract either module depends on.
 */
@Slf4j
@Component
@Profile("dev")
public class MockAIProvider implements AIProvider {

    private static final String PROVIDER_NAME = "mock";
    private static final String MOCK_MODEL = "mock-dev-provider";

    /** Include this token in any context field to deliberately exercise the failure path in dev. */
    private static final String FAILURE_TRIGGER = "simulate_failure";

    private static final Pattern CONTEXT_LINE = Pattern.compile(
            "(?:Media description|Additional notes|Event details|Achievement|Competition results|"
                    + "Training session|Coach notes):\\s*(\\S.*)");

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public AIGenerationResult generate(AIGenerationRequest request) {
        String systemPrompt = nullToEmpty(request.systemPrompt());
        String userPrompt = nullToEmpty(request.userPrompt());
        String combinedLower = (systemPrompt + " " + userPrompt).toLowerCase();

        if (combinedLower.contains(FAILURE_TRIGGER)) {
            throw new AIProviderException(AIProviderException.Reason.UNAVAILABLE,
                    "Mock provider: simulated failure (" + FAILURE_TRIGGER + " marker present in context)");
        }

        simulateLatency();

        String context = extractContext(userPrompt);
        String content = buildContent(combinedLower, context);
        log.info("MockAIProvider generated {} chars of dev sample content", content.length());

        return AIGenerationResult.builder()
                .rawContent(content)
                .modelUsed(MOCK_MODEL)
                .build();
    }

    /** A short, artificial delay so loading states in the UI look and behave like a real API call. */
    private void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(300, 900));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String extractContext(String userPrompt) {
        for (String line : userPrompt.split("\n")) {
            Matcher matcher = CONTEXT_LINE.matcher(line.trim());
            if (matcher.matches()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    private String buildContent(String combinedLower, String context) {
        if (combinedLower.contains("relevant hashtags mixing branded")) {
            return hashtags();
        }
        if (combinedLower.contains("instagram/facebook reels")) {
            return reelScript(context);
        }
        if (combinedLower.contains("youtube shorts/tiktok-style")) {
            return shortVideoScript(context);
        }
        if (combinedLower.contains("5 to 7 slide")) {
            return carousel(context);
        }
        if (combinedLower.contains("400-600 word article")) {
            return blogArticle(context);
        }
        if (combinedLower.contains("linkedin post aimed at sponsors")) {
            return linkedInPost(context);
        }
        if (combinedLower.contains("facebook community of parents")) {
            return facebookCaption(context);
        }
        if (combinedLower.contains("engaging, authentic instagram captions")) {
            return instagramCaption(context);
        }
        if (combinedLower.contains("concise, keyword-rich page title")) {
            return seoTitle();
        }
        if (combinedLower.contains("150 and 160 characters")) {
            return seoDescription();
        }
        if (combinedLower.contains("neutral meta description")) {
            return metaDescription();
        }
        if (combinedLower.contains("8 to 15 relevant search keywords")) {
            return keywords();
        }
        if (combinedLower.contains("call-to-action sentence")) {
            return callToAction();
        }
        if (combinedLower.contains("student or team achievement")) {
            return achievementAnnouncement(context);
        }
        if (combinedLower.contains("promoting an upcoming event")) {
            return eventAnnouncement(context);
        }
        if (combinedLower.contains("discipline, focus, and mental toughness")) {
            return motivationalPost();
        }
        if (combinedLower.contains("practical training tip")) {
            return trainingTip();
        }
        return genericCaption(context);
    }

    private String hashtags() {
        return "#ArjunSportsAcademy #ShootingSports #Marksmanship #YouthAthletes #PrecisionTraining "
                + "#ChampionMindset #RegionalChampions #TrainHard #FocusAndDiscipline #ShootingRange "
                + "#NextGenAthletes #SportsExcellence";
    }

    private String reelScript(String context) {
        String hook = context != null ? context : "Every champion starts with a single steady shot.";
        return """
                Scene 1 (0-3s): Close-up of hands loading a rifle in slow motion - "Precision starts here."
                Scene 2 (3-8s): Athlete taking aim, steady breathing visible - "%s"
                Scene 3 (8-13s): Shot fired, target hit dead center, slow-motion recoil - "Bullseye."
                Scene 4 (13-20s): Team celebrating, coach high-fiving athlete - "This is what dedication looks like."
                Scene 5 (20-27s): Academy logo overlaid on athletes training - "Arjun Sports Shooting Academy - Train Like a Champion."
                Scene 6 (27-30s): Text overlay with CTA - "Book your trial session - link in bio."
                """.formatted(hook);
    }

    private String shortVideoScript(String context) {
        String story = context != null ? context : "Weeks of quiet, unglamorous practice.";
        return """
                Scene 1 (0-5s) [HOOK]: Athlete narrowly misses a shot, visible frustration - "Failure is part of the process."
                Scene 2 (5-15s) [STORY]: Montage of practice sessions, coach giving feedback - "%s"
                Scene 3 (15-30s) [STORY]: Same athlete competing months later, focused and calm - "Then it clicks."
                Scene 4 (30-45s) [STORY]: Athlete hits the target, quiet celebration - "Not talent. Repetition."
                Scene 5 (45-55s) [STORY]: Short interview snippet with the athlete - "It's about showing up when it's hard."
                Scene 6 (55-60s) [CTA]: Academy branding on screen - "Your journey starts at Arjun Sports Shooting Academy. Enroll today."
                """.formatted(story);
    }

    private String carousel(String context) {
        String hook = context != null ? context : "From first-timer to champion - here's how it happens.";
        return """
                Slide 1: %s 🎯
                Slide 2: Step 1 - Master the fundamentals: stance, grip, breathing.
                Slide 3: Step 2 - Build consistency through structured daily practice.
                Slide 4: Step 3 - Compete locally to build real pressure experience.
                Slide 5: Step 4 - Refine technique with personalized coach feedback.
                Slide 6: Step 5 - Compete regionally and nationally with confidence.
                Slide 7: Ready to start your journey? Enroll at Arjun Sports Shooting Academy today!
                """.formatted(hook);
    }

    private String blogArticle(String context) {
        String opening = context != null
                ? context
                : "our athletes continue to prove that discipline and preparation outperform raw talent every time";
        return """
                At Arjun Sports Shooting Academy, %s. This week was no exception, and it's a reminder of why \
                we built this program around structure, patience, and measurable progress rather than shortcuts.

                Competitive shooting sports demand a rare combination of physical stillness and mental sharpness. \
                Our coaching staff works with every athlete individually, building a training plan around their \
                current fundamentals - stance, grip, breath control, and follow-through - before layering on the \
                pressure of live competition. It is slow, deliberate work, and it is exactly why our athletes are \
                ready when the pressure is highest.

                None of this happens without the support of our academy community - parents who show up for early \
                morning sessions, alumni who return to mentor newer athletes, and local partners who believe in \
                what we're building. If you'd like to see the program in action, we'd love to have you visit for \
                a session.

                Ready to start your own journey in competitive shooting sports? Get in touch with Arjun Sports \
                Shooting Academy today to book a trial session.
                """.formatted(opening);
    }

    private String linkedInPost(String context) {
        String body = context != null
                ? context
                : "another milestone that reflects months of disciplined preparation";
        return """
                At Arjun Sports Shooting Academy, we believe competitive shooting sports build more than \
                marksmanship - they build discipline, focus, and leadership that carries well beyond the range.

                %s. Achievements like this don't happen by accident; they are the result of a structured program, \
                dedicated coaching, and athletes willing to put in the repetitions nobody sees.

                We're proud to partner with sponsors and supporters who share our commitment to developing \
                well-rounded young athletes ready to lead both on and off the range.
                """.formatted(body);
    }

    private String facebookCaption(String context) {
        String body = context != null ? context : "another proud moment for our academy family";
        return """
                We want to take a moment to celebrate something special: %s

                This is exactly why we do what we do - watching our athletes grow in skill, confidence, and \
                character, one session at a time. Thank you to every parent, alumnus, and supporter who's part \
                of this journey with us. We'd love to hear your thoughts in the comments! 🎯
                """.formatted(body);
    }

    private String instagramCaption(String context) {
        String body = context != null ? context : "another quiet morning of focus, breath, and precision";
        return "🎯 " + body + "\n\nEvery rep, every round, every quiet moment of focus adds up. That's the "
                + "discipline behind every champion at Arjun Sports Shooting Academy. Keep aiming higher. 🔥";
    }

    private String seoTitle() {
        return "Arjun Sports Shooting Academy | Elite Youth Training";
    }

    private String seoDescription() {
        return "Discover Arjun Sports Shooting Academy's championship-winning youth shooting programs. Book a "
                + "trial session today and start your journey to the podium.";
    }

    private String metaDescription() {
        return "Arjun Sports Shooting Academy offers structured training programs, competition preparation, "
                + "and coaching for youth shooting sports athletes.";
    }

    private String keywords() {
        return "youth shooting academy, competitive shooting training, junior marksmanship program, shooting "
                + "sports coaching, regional shooting championship, air rifle training, precision shooting for "
                + "kids, shooting sports scholarship, beginner shooting lessons, elite shooting academy";
    }

    private String callToAction() {
        return "Ready to take your first shot at greatness? Book your trial session at Arjun Sports Shooting "
                + "Academy today!";
    }

    private String achievementAnnouncement(String context) {
        String achievement = context != null ? context : "our athlete's incredible achievement";
        return "🏆 Huge congratulations on " + achievement + "! This is a direct reflection of countless hours "
                + "of practice, unwavering focus, and true grit. The entire Arjun Sports Shooting Academy family "
                + "couldn't be prouder. Way to represent, champion!";
    }

    private String eventAnnouncement(String context) {
        String event = context != null ? context : "our upcoming academy showcase";
        return "📅 Mark your calendars! " + event + " is coming up, and we can't wait to see our athletes "
                + "compete. Come cheer on our team, meet fellow academy families, and witness some incredible "
                + "marksmanship. See you there!";
    }

    private String motivationalPost() {
        return "Discipline isn't about perfection - it's about showing up, especially on the days it's hardest. "
                + "Every great shot you'll ever take starts with a decision to keep going when it would be easier "
                + "to stop. Stay focused. Stay grounded. Your breakthrough is closer than you think.";
    }

    private String trainingTip() {
        return "🎯 Coach's Tip: Focus on your breathing rhythm before every shot - inhale, exhale halfway, then "
                + "hold and squeeze the trigger during your natural pause. Consistency in your breathing routine "
                + "builds consistency in your groupings. Practice this drill for 10 minutes daily.";
    }

    private String genericCaption(String context) {
        String body = context != null ? context : "the dedication our athletes bring to the range every day";
        return "At Arjun Sports Shooting Academy, we're proud of " + body + ". Discipline, focus, and community "
                + "- that's what we're all about.";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
