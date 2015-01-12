/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import org.mule.extension.validation.Locale;
import org.mule.extensions.annotations.Parameter;
import org.mule.extensions.annotations.param.Optional;


public final class NumberValidationOptions
{

    /**
     * Value to validate
     */
    @Parameter
    private String value;

    /**
     * The locale to use for the format
     */
    @Parameter
    @Optional(defaultValue = "US")
    private Locale locale;

    /**
     * The pattern used to format the value
     */
    @Parameter
    @Optional
    private String pattern;

    /**
     * The minimum value
     */
    @Parameter
    @Optional
    private Double minValue;

    /**
     * The maximum value
     */
    @Parameter
    @Optional
    private Double maxValue;


    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public Locale getLocale()
    {
        return locale;
    }

    public void setLocale(Locale locale)
    {
        this.locale = locale;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    public Double getMinValue()
    {
        return minValue;
    }

    public void setMinValue(Double minValue)
    {
        this.minValue = minValue;
    }

    public Double getMaxValue()
    {
        return maxValue;
    }

    public void setMaxValue(Double maxValue)
    {
        this.maxValue = maxValue;
    }
}
