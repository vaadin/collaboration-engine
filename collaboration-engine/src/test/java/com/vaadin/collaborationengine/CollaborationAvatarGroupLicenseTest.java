package com.vaadin.collaborationengine;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.CollaborationAvatarGroupTest.AvatarGroupTestClient;
import com.vaadin.collaborationengine.util.TestUtils;

public class CollaborationAvatarGroupLicenseTest extends AbstractLicenseTest {

    private AvatarGroupTestClient client1;
    private AvatarGroupTestClient client2;
    private AvatarGroupTestClient client3;

    @Override
    @Before
    public void init() throws IOException {
        super.init();

        client1 = new AvatarGroupTestClient(1, ceSupplier);
        client1.attach();
        client2 = new AvatarGroupTestClient(2, ceSupplier);
        client2.attach();

        fillGraceQuota();
        client3 = new AvatarGroupTestClient(3, ceSupplier);
        client3.attach();
    }

    @Test
    public void licenseTermsExceeded_attachGroup_onlyLocalAvatarDisplayed() {
        Assert.assertEquals(TestUtils.newHashSet("name3"),
                client3.getItemNames());
    }

    @Test
    public void licenseTermsExceeded_attachGroup_avatarNotDisplayedToOthers() {
        Assert.assertEquals(TestUtils.newHashSet("name1", "name2"),
                client1.getItemNames());
    }

    @Test
    public void licenseTermsExceeded_activeGroupsKeepUpdating() {
        client2.detach();
        Assert.assertEquals(TestUtils.newHashSet("name1"),
                client1.getItemNames());
        client2.attach();
        Assert.assertEquals(TestUtils.newHashSet("name1", "name2"),
                client1.getItemNames());
    }
}
