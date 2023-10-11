package com.vaadin.collaborationengine;

import static org.hamcrest.CoreMatchers.startsWith;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.collaborationengine.util.TestUtils;

public class BeaconHandlerTest {
    @Test
    public void createBeaconUrl_pathUsedCorrectly() {
        BeaconHandler beaconHandler = new BeaconHandler("/foo");
        Assert.assertThat(beaconHandler.createBeaconUrl(),
                startsWith("./foo?"));
    }

    @Test
    public void serializeBeaconHandler() {
        BeaconHandler beaconHandler = new BeaconHandler("/foo");

        BeaconHandler deserializedBeaconHandler = TestUtils
                .serialize(beaconHandler);
    }
}
