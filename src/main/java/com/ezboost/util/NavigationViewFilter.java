package com.ezboost.util;

import com.ezboost.dao.RoomDataDAO;
import com.ezboost.dao.SeasonalityDAO;
import com.ezboost.model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** Supplies navigation state to JSPs without performing data access in the view. */
public class NavigationViewFilter implements Filter {

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            prepare((HttpServletRequest) request);
        }
        chain.doFilter(request, response);
    }

    private void prepare(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        String currentStep = OnboardingUtil.getCurrentStep(user);
        boolean onboarding = OnboardingUtil.isOnboardingActive(user);
        String currentUrl = OnboardingUtil.getStepUrl(currentStep);
        String homeLink = "homepage.jsp";
        String boostLink = "DataImport";

        if (user != null) {
            boolean ready = RoomDataDAO.hasRoomData(user.getUserId())
                    && !SeasonalityDAO.getMonthlyDataByUser(user.getUserId()).isEmpty()
                    && SeasonalityDAO.getThresholdsByUser(user.getUserId()) != null;
            boostLink = ready ? "BoostMe" : "DataImport";
        }
        if (onboarding) {
            homeLink = currentUrl;
            boostLink = currentUrl;
        }

        request.setAttribute("navHomeLink", homeLink);
        request.setAttribute("navBoostLink", boostLink);
        request.setAttribute("navOnboardingActive", onboarding);
        request.setAttribute("navOnboardingSteps", OnboardingUtil.STEP_ORDER);
        request.setAttribute("navOnboardingCurrentIndex", Math.max(0, OnboardingUtil.STEP_ORDER.indexOf(currentStep)));
        request.setAttribute("navOnboardingStepLabel", OnboardingUtil.getStepLabel(currentStep));
        request.setAttribute("navImportHref", lockedUrl(onboarding, currentStep, OnboardingUtil.STEP_IMPORT, currentUrl, "DataImport"));
        request.setAttribute("navMultiplierHref", lockedUrl(onboarding, currentStep, OnboardingUtil.STEP_MULTIPLIERS, currentUrl, "MultiplierSettings"));
        request.setAttribute("navEventsHref", lockedUrl(onboarding, currentStep, OnboardingUtil.STEP_EVENTS, currentUrl, "EventSettings"));
        request.setAttribute("navSegmentsHref", lockedUrl(onboarding, currentStep, OnboardingUtil.STEP_SEGMENTS, currentUrl, "SegmentSettings"));
        request.setAttribute("navProfileHref", onboarding ? currentUrl : "Profile");
        request.setAttribute("navImportClass", navClass(onboarding, currentStep, OnboardingUtil.STEP_IMPORT));
        request.setAttribute("navMultiplierClass", navClass(onboarding, currentStep, OnboardingUtil.STEP_MULTIPLIERS));
        request.setAttribute("navEventsClass", navClass(onboarding, currentStep, OnboardingUtil.STEP_EVENTS));
        request.setAttribute("navSegmentsClass", navClass(onboarding, currentStep, OnboardingUtil.STEP_SEGMENTS));
        request.setAttribute("navBoostClass", onboarding ? "nav-link nav-link-locked" : "nav-link");
        request.setAttribute("navProfileClass", onboarding ? "nav-link nav-link-locked" : "nav-link");
    }

    private String lockedUrl(boolean onboarding, String currentStep, String targetStep, String currentUrl, String defaultUrl) {
        return onboarding && !targetStep.equals(currentStep) ? currentUrl : defaultUrl;
    }

    private String navClass(boolean onboarding, String currentStep, String targetStep) {
        return onboarding && !targetStep.equals(currentStep) ? "nav-link nav-link-locked" : "nav-link";
    }
}
