package com.ezboost.service;

import com.ezboost.dao.RoomDataDAO;
import com.ezboost.dao.SeasonalityDAO;
import com.ezboost.model.User;
import jakarta.servlet.http.HttpServletRequest;

/** Prepares the setup state rendered by the BoostMe page. */
public final class BoostMePageService {

    private BoostMePageService() {
    }

    public static void prepare(HttpServletRequest request, User user) {
        int roomTypeCount = 0;
        int monthlyCount = 0;
        boolean readyForOptimization = false;

        if (user != null) {
            roomTypeCount = RoomDataDAO.getRoomTypeCount(user.getUserId());
            monthlyCount = SeasonalityDAO.getMonthlyDataByUser(user.getUserId()).size();
            readyForOptimization = roomTypeCount > 0
                    && monthlyCount > 0
                    && SeasonalityDAO.getThresholdsByUser(user.getUserId()) != null;
        }

        request.setAttribute("roomTypeCount", roomTypeCount);
        request.setAttribute("monthlyCount", monthlyCount);
        request.setAttribute("readyForOptimization", readyForOptimization);
    }
}
