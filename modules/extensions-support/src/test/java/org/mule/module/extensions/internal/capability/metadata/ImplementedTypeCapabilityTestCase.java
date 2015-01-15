/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.capability.metadata;

import static org.junit.Assert.assertSame;
import org.mule.extensions.annotations.Extensible;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ImplementedTypeCapabilityTestCase extends AbstractMuleTestCase
{

    @Test
    public void extensible()
    {
        ImplementedTypeCapability capability = new ImplementedTypeCapability(TestExtensibleType.class);
        assertSame(capability.getType(), TestExtensibleType.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void notExtensible()
    {
        new ImplementedTypeCapability(Object.class);
    }

    @Extensible
    private class TestExtensibleType
    {

    }
}
