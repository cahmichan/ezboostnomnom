package com.ezboost.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.SessionCookieConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Applies runtime cookie policy and closes pooled resources during redeploy. */
public class ApplicationLifecycleListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationLifecycleListener.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        SessionCookieConfig cookie = event.getServletContext().getSessionCookieConfig();
        cookie.setHttpOnly(true);
        cookie.setSecure(AppConfig.secureCookies());
        logger.info("EzBoost starting in {} mode", AppConfig.production() ? "production" : "development");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        DBConnection.close();
    }
}
