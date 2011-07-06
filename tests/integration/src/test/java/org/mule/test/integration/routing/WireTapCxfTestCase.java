/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.api.context.notification.ServerNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.IntegrationDynamicPortTestCase;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.util.concurrent.Latch;

public class WireTapCxfTestCase extends IntegrationDynamicPortTestCase
{
    static final Latch tapLatch = new Latch();

    public WireTapCxfTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/routing/wire-tap-cxf-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/routing/wire-tap-cxf-flow.xml"}});
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        muleContext.registerListener(new FunctionalTestNotificationListener()
        {
            public void onNotification(ServerNotification notification)
            {
                tapLatch.release();
            }
        });
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }

    @Test
    public void testWireTap() throws Exception
    {
        String url = "http://localhost:" + getPorts().get(0) + "/services/EchoUMO";
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body><echo><text>foo</text></echo></soap:Body></soap:Envelope>";

        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send(url, msg, null);
        assertNotNull(response);

        String responseString = response.getPayloadAsString();
        assertTrue(responseString.contains("echoResponse"));
        assertFalse(responseString.contains("soap:Fault"));

        assertTrue(tapLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
