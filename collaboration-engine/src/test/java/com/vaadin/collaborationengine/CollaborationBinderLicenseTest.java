package com.vaadin.collaborationengine;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.AbstractCollaborationBinderTest.BinderTestClient;
import com.vaadin.collaborationengine.util.TestBean;

public class CollaborationBinderLicenseTest extends AbstractLicenseTest {

    private BinderTestClient client1;
    private BinderTestClient client2;
    private BinderTestClient client3;

    @Before
    public void init() throws IOException {
        super.init();
        client1 = addClient();
        client1.field.setValue("value-from-client-1");
        client2 = addClient();

        fillGraceQuota();
        client3 = addClient();
    }

    @Test
    public void licenseTermsExceeded_addClient_fieldsPopulatedFromBeanSupplier() {
        Assert.assertEquals("value-from-bean", client3.field.getValue());
    }

    @Test
    public void licenseTermsExceeded_addClient_valuesNotPropagated() {
        client2.field.setValue("value-from-client-2");
        Assert.assertEquals("value-from-bean", client3.field.getValue());

        client3.field.setValue("value-from-client-3");
        Assert.assertEquals("value-from-client-2", client2.field.getValue());
    }

    @Test
    public void licenseTermsExceeded_addClient_activeClientsRemainActive() {
        client2.field.setValue("value-from-client-2");
        Assert.assertEquals("value-from-client-2", client1.field.getValue());
    }

    @Test
    public void licenseTermsExceeded_setTopicBeforeAttach_fieldsPopulated() {
        BinderTestClient client = new BinderTestClient(ce);
        client.bind();
        client.binder.setTopic("foo", () -> new TestBean("bean-value"));
        client.attach();
        Assert.assertEquals("bean-value", client.field.getValue());
    }

    private BinderTestClient addClient() {
        BinderTestClient client = new BinderTestClient(ce);
        client.attach();
        client.bind();
        client.binder.setTopic("foo", () -> new TestBean("value-from-bean"));
        return client;
    }
}
