<%@ page import="com.ezboost.model.User" %>
<%@ page import="com.ezboost.util.OnboardingUtil" %>
<%
    User onboardingUser = (User) session.getAttribute("user");
    boolean guidedSetupActive = OnboardingUtil.isOnboardingActive(onboardingUser);
    String guidedCurrentStep = OnboardingUtil.getCurrentStep(onboardingUser);
    int guidedCurrentIndex = OnboardingUtil.STEP_ORDER.indexOf(guidedCurrentStep);
    String[] guidedSteps = {
            OnboardingUtil.STEP_IMPORT,
            OnboardingUtil.STEP_MULTIPLIERS,
            OnboardingUtil.STEP_EVENTS,
            OnboardingUtil.STEP_SEGMENTS
    };
%>
<% if (guidedSetupActive) { %>
<section class="onboarding-progress" aria-label="Guided setup progress">
    <div class="onboarding-progress-copy">
        <span class="onboarding-kicker">Guided Setup</span>
        <strong>Step <%= guidedCurrentIndex + 1 %> of <%= guidedSteps.length %>: <%= OnboardingUtil.getStepLabel(guidedCurrentStep) %></strong>
        <span>Complete this step to unlock the next workspace.</span>
    </div>
    <ol class="onboarding-steps">
        <% for (int i = 0; i < guidedSteps.length; i++) {
            String step = guidedSteps[i];
            String state = i < guidedCurrentIndex ? "complete" : (i == guidedCurrentIndex ? "current" : "locked");
        %>
        <li class="onboarding-step <%= state %>">
            <span class="onboarding-step-index"><%= i + 1 %></span>
            <span class="onboarding-step-label"><%= OnboardingUtil.getStepLabel(step) %></span>
        </li>
        <% } %>
    </ol>
</section>
<% } %>
