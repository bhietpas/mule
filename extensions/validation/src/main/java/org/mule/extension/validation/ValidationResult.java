/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.validation;

import org.mule.api.MuleEvent;

import java.io.Serializable;

public class ValidationResult implements Serializable
{

    private final MuleEvent muleEvent;
    private final Validator validator;
    private final boolean error;
    private final String message;

    public ValidationResult(MuleEvent muleEvent, Validator validator, boolean error, String message)
    {
        this.muleEvent = muleEvent;
        this.validator = validator;
        this.error = error;
        this.message = message;
    }

    public String getMessage()
    {
        return message;
    }

    public boolean isError()
    {
        return error;
    }

    public MuleEvent getMuleEvent()
    {
        return muleEvent;
    }

    public Validator getValidator()
    {
        return validator;
    }
}
