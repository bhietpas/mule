/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import org.mule.api.MuleEvent;
import org.mule.api.NestedProcessor;
import org.mule.extension.validation.ValidationResult;
import org.mule.extension.validation.exception.ValidationResultException;
import org.mule.extensions.annotations.Operation;
import org.mule.extensions.annotations.param.Optional;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class AllValidation extends ValidationSupport
{

    @Inject
    private MuleEvent muleEvent;

    @Operation
    public MuleEvent all(List<NestedProcessor> validations, @Optional(defaultValue = "true") boolean throwsException) throws Exception
    {
        List<String> messages = new ArrayList<>(validations.size());
        for (NestedProcessor validation : validations)
        {
            try
            {
                validation.process();
            }
            catch (Exception e)
            {
                messages.add(e.getMessage());
            }
        }

        ValidationResult result = null; //new ValidationResult(muleEvent, null, false, ""messages);

        if (throwsException)
        {
            throw new ValidationResultException(result);
        }
        else
        {
            muleEvent.getMessage().setPayload(result);
        }

        return muleEvent;
    }
}
