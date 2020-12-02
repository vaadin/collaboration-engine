package com.vaadin.collaborationengine;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.CollaborationAvatarGroupTest.AvatarGroupTestClient;

public class CollaborationAvatarGroupLicenseTest extends AbstractLicenseTest {

    private AvatarGroupTestClient client1;
    private AvatarGroupTestClient client2;
    private AvatarGroupTestClient client3;

    @Before
    public void init() throws IOException {
        super.init();

        client1 = new AvatarGroupTestClient(1, ce);
        client1.attach();
        client2 = new AvatarGroupTestClient(2, ce);
        client2.attach();

        fillGraceQuota();
        client3 = new AvatarGroupTestClient(3, ce);
        client3.attach();
    }

    @Test
    public void licenseTermsExceeded_attachGroup_onlyLocalAvatarDisplayed() {
        Assert.assertEquals(Arrays.asList("name3"), client3.getItemNames());
    }

    @Test
    public void licenseTermsExceeded_attachGroup_avatarNotDisplayedToOthers() {
        Assert.assertEquals(Arrays.asList("name1", "name2"),
                client1.getItemNames());
    }

    @Test
    public void licenseTermsExceeded_activeGroupsKeepUpdating() {
        client2.detach();
        Assert.assertEquals(Arrays.asList("name1"), client1.getItemNames());
        client2.attach();
        Assert.assertEquals(Arrays.asList("name1", "name2"),
                client1.getItemNames());
    }
}
