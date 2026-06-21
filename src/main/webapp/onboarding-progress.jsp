<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:if test="${navOnboardingActive}">
<section class="onboarding-progress" aria-label="Guided setup progress">
    <div class="onboarding-progress-copy">
        <span class="onboarding-kicker">Guided Setup</span>
        <strong>Step ${navOnboardingCurrentIndex + 1} of ${fn:length(navOnboardingSteps)}: ${navOnboardingStepLabel}</strong>
        <span>Complete this step to unlock the next workspace.</span>
    </div>
    <ol class="onboarding-steps">
        <c:forEach var="step" items="${navOnboardingSteps}" varStatus="status">
        <li class="onboarding-step ${status.index < navOnboardingCurrentIndex ? 'complete' : status.index == navOnboardingCurrentIndex ? 'current' : 'locked'}">
            <span class="onboarding-step-index">${status.index + 1}</span>
            <span class="onboarding-step-label">${step}</span>
        </li>
        </c:forEach>
    </ol>
</section>
</c:if>
