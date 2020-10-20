package com.vaadin.collaborationengine;

import java.time.YearMonth;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.EagerConnectionContext;

public class StatisticsTest {
    YearMonth configuredYearMonth = YearMonth.of(2020, 5);

    public class StatisticsWithMonthControl extends Statistics {

        @Override
        protected YearMonth getCurrentMonth() {
            return configuredYearMonth;
        }

        protected void setCurrentMonth(int year, int month) {
            configuredYearMonth = YearMonth.of(year, month);
        }
    }

    private StatisticsWithMonthControl statistics;

    @Before
    public void init() {
        statistics = new StatisticsWithMonthControl();
    }

    @Test
    public void registerUser_addUsers_usersAdded() {
        Assert.assertTrue(this.statistics.getStatistics().isEmpty());
        this.statistics.registerUser("steve");

        Map<YearMonth, Set<String>> statistics = this.statistics
                .getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.keySet().contains(configuredYearMonth));
        Assert.assertEquals(1, statistics.get(configuredYearMonth).size());
        Assert.assertTrue(
                statistics.get(configuredYearMonth).contains("steve"));
        this.statistics.registerUser("bob");

        statistics = this.statistics.getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.keySet().contains(configuredYearMonth));
        Assert.assertEquals(2, statistics.get(configuredYearMonth).size());
        Assert.assertTrue(
                statistics.get(configuredYearMonth).contains("steve"));
        Assert.assertTrue(statistics.get(configuredYearMonth).contains("bob"));
    }

    @Test
    public void registerUser_addSameUserTwice_userAddedOnlyOnce() {
        statistics.registerUser("steve");
        statistics.registerUser("bob");
        statistics.registerUser("steve");
        Map<YearMonth, Set<String>> statistics = this.statistics
                .getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.keySet().contains(configuredYearMonth));
        Set<String> usersInMonth = statistics.get(configuredYearMonth);
        Assert.assertEquals(2, usersInMonth.size());
        Assert.assertTrue(usersInMonth.contains("steve"));
        Assert.assertTrue(usersInMonth.contains("bob"));
    }

    @Test
    public void registerUser_monthChanges_userIsReAdded() {
        statistics.registerUser("steve");
        statistics.registerUser("bob");
        statistics.setCurrentMonth(2020, 6);
        statistics.registerUser("steve");

        Map<YearMonth, Set<String>> statistics = this.statistics
                .getStatistics();
        YearMonth firstMonth = YearMonth.of(2020, 5);
        YearMonth secondMonth = YearMonth.of(2020, 6);
        Assert.assertEquals(2, statistics.keySet().size());
        Assert.assertTrue(statistics.keySet().contains(firstMonth));
        Assert.assertEquals(2, statistics.get(firstMonth).size());
        Assert.assertTrue(statistics.get(firstMonth).contains("steve"));
        Assert.assertTrue(statistics.get(firstMonth).contains("bob"));
        Assert.assertEquals(1, statistics.get(secondMonth).size());
        Assert.assertTrue(statistics.get(secondMonth).contains("steve"));
    }

    @Test
    public void openTopicConnection_userRegistered() {
        CollaborationEngine engine = new CollaborationEngine();
        engine.openTopicConnection(new EagerConnectionContext(), "foo",
                new UserInfo("steve"), topicConnection -> null);
        Map<YearMonth, Set<String>> statistics = engine.getStatistics()
                .getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.keySet().contains(YearMonth.now()));
        Set<String> currentMonth = statistics.get(YearMonth.now());
        Assert.assertEquals(1, currentMonth.size());
        Assert.assertTrue(currentMonth.contains("steve"));
    }
}
