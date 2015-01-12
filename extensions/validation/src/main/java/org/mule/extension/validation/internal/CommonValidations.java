/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import org.mule.extension.validation.CreditCardType;
import org.mule.extension.validation.Locale;
import org.mule.extension.validation.Validator;
import org.mule.extensions.annotations.Operation;
import org.mule.extensions.annotations.param.Optional;
import org.mule.mvel2.compiler.BlankLiteral;
import org.mule.transport.NullPayload;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.CodeValidator;
import org.apache.commons.validator.routines.CreditCardValidator;
import org.apache.commons.validator.routines.DateValidator;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.DoubleValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.FloatValidator;
import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.IntegerValidator;
import org.apache.commons.validator.routines.LongValidator;
import org.apache.commons.validator.routines.RegexValidator;
import org.apache.commons.validator.routines.ShortValidator;
import org.apache.commons.validator.routines.TimeValidator;
import org.apache.commons.validator.routines.UrlValidator;

public final class CommonValidations extends ValidationSupport
{

    /**
     * Validates that the given {@code value} is {@code true}
     *
     * @param expression the boolean to test
     * @throws Exception if the value is not {@code true}
     */
    @Operation
    public void isTrue(final boolean expression) throws Exception
    {
        validateWith(new Validator()
        {
            @Override
            public boolean isValid()
            {
                return expression;
            }

            @Override
            public String getErrorMessage()
            {
                return "value was false";
            }
        });
    }

    /**
     * Validates that the given {@code value} is {@code false}
     *
     * @param expression the boolean to test
     * @throws Exception if the value is not {@code true}
     */
    @Operation
    public void isFalse(final boolean expression) throws Exception
    {
        validateWith(new Validator()
        {
            @Override
            public boolean isValid()
            {
                return !expression;
            }

            @Override
            public String getErrorMessage()
            {
                return "value was true";
            }
        });
    }

    /**
     * Fails if {@code creditCardNumber} is not a valid credit card number throw an exception.
     *
     * @param creditCardNumber the credit card number to validate
     * @param creditCardType   the card's type
     */
    @Operation
    public void creditCard(final String creditCardNumber, final CreditCardType creditCardType) throws Exception
    {
        validateWith(new Validator()
        {

            @Override
            public boolean isValid()
            {
                CreditCardValidator validator = new CreditCardValidator(new CodeValidator[] {creditCardType.getCodeValidator()});
                return validator.validate(creditCardNumber) != null;
            }

            @Override
            public String getErrorMessage()
            {
                return String.format("Credit card number %s is not valid for card type: %s", creditCardNumber, creditCardType);
            }
        });
    }

    /**
     * Checks that a {@code date} in {@link String} format is valid for the given {@code pattern} and {@code locale}.
     * If no pattern is provided, then the {@code locale}'s default will be used
     *
     * @param date    A date in String format
     * @param locale  the locale of the String
     * @param pattern the pattern for the {@code date}
     */
    @Operation
    public void date(final String date, @Optional(defaultValue = "US") final Locale locale, final String pattern) throws Exception
    {
        validateWith(new Validator()
        {

            @Override
            public boolean isValid()
            {
                DateValidator validator = DateValidator.getInstance();

                if (pattern != null)
                {
                    return validator.isValid(date, pattern, locale.getJavaLocale());
                }
                else
                {
                    return validator.isValid(date, locale.getJavaLocale());
                }
            }

            @Override
            public String getErrorMessage()
            {
                return String.format("The date %s is not valid under the pattern '%s' for the locale %s", date, pattern, locale);
            }
        });
    }

    /**
     * Validates that the specified {@code domain} is a valid one name with a
     * recognized top-level domain.
     *
     * @param domain the domain name to validate
     */
    @Operation
    public void domain(final String domain) throws Exception
    {
        validateWith(new Validator()
        {

            @Override
            public boolean isValid()
            {
                return DomainValidator.getInstance().isValid(domain);
            }

            @Override
            public String getErrorMessage()
            {
                return domain + " is not a valid domain";
            }
        });
    }

    /**
     * Receives a numeric {@code value} as a {@link String} and checks that it can be parsed as a {@link Double}
     *
     * @param options the number options
     */
    @Operation
    public void isDouble(NumberValidationOptions options) throws Exception
    {
        validateWith(new NumberValidator(options)
        {
            @Override
            protected Number validateWithPattern(String value, String pattern, Locale locale)
            {
                return DoubleValidator.getInstance().validate(value, pattern, locale.getJavaLocale());
            }

            @Override
            protected Number validateWithoutPattern(String value, Locale locale)
            {
                return DoubleValidator.getInstance().validate(value, locale.getJavaLocale());
            }

            @Override
            protected Class<? extends Number> getNumberType()
            {
                return Double.class;
            }
        });
    }

    /**
     * Validates that the {@code email} address is valid
     *
     * @param email an email address
     */
    @Operation
    public void email(final String email) throws Exception
    {
        validateWith(new Validator()
        {

            @Override
            public boolean isValid()
            {
                return EmailValidator.getInstance().isValid(email);
            }

            @Override
            public String getErrorMessage()
            {
                return email + " is not a valid email address";
            }
        });
    }

    /**
     * Receives a numeric {@code value} as a {@link String} and checks that it can be parsed as a {@link Float}
     *
     * @param options the number options
     */
    @Operation
    public void isFloat(NumberValidationOptions options) throws Exception
    {
        validateWith(new NumberValidator(options)
        {
            @Override
            protected Number validateWithPattern(String value, String pattern, Locale locale)
            {
                return FloatValidator.getInstance().validate(value, pattern, locale.getJavaLocale());
            }

            @Override
            protected Number validateWithoutPattern(String value, Locale locale)
            {
                return FloatValidator.getInstance().validate(value, locale.getJavaLocale());
            }

            @Override
            protected Class<? extends Number> getNumberType()
            {
                return Float.class;
            }
        });
    }

    /**
     * Receives a numeric {@code value} as a {@link String} and checks that it can be parsed as a {@link Integer}
     *
     * @param options the number options
     */
    @Operation
    public void isInteger(NumberValidationOptions options) throws Exception
    {
        validateWith(new NumberValidator(options)
        {
            @Override
            protected Number validateWithPattern(String value, String pattern, Locale locale)
            {
                return IntegerValidator.getInstance().validate(value, pattern, locale.getJavaLocale());
            }

            @Override
            protected Number validateWithoutPattern(String value, Locale locale)
            {
                return IntegerValidator.getInstance().validate(value, locale.getJavaLocale());
            }

            @Override
            protected Class<? extends Number> getNumberType()
            {
                return Integer.class;
            }
        });
    }

    /**
     * Validates that an {@code ip} address represented as a {@link String} is valid
     *
     * @param ip the ip address to validate
     */
    @Operation
    public void ip(final String ip) throws Exception
    {
        validateWith(new Validator()
        {

            @Override
            public boolean isValid()
            {
                return InetAddressValidator.getInstance().isValid(ip);
            }

            @Override
            public String getErrorMessage()
            {
                return ip + " is not a valid ip address";
            }
        });
    }

    /**
     * Validates that the supplied {@code isbn} is a valid ISBN10 code
     *
     * @param isbn the code to validate
     */
    @Operation
    public void isbn10(final String isbn) throws Exception
    {
        validateWith(new Validator()
        {

            @Override
            public boolean isValid()
            {
                return ISBNValidator.getInstance().isValidISBN10(isbn);
            }

            @Override
            public String getErrorMessage()
            {
                return isbn + " is not a valid ISBN10 code";
            }
        });
    }

    /**
     * Validates that the supplied {@code isbn} is a valid ISBN13 code
     *
     * @param isbn the code to validate
     */
    @Operation
    public void isbn13(final String isbn) throws Exception
    {
        validateWith(new Validator()
        {

            @Override
            public boolean isValid()
            {
                return ISBNValidator.getInstance().isValidISBN13(isbn);
            }

            @Override
            public String getErrorMessage()
            {
                return isbn + " is not a valid ISBN13 code";
            }
        });
    }

    /**
     * Validates that {@code value} has a length between certain inclusive boundaries
     *
     * @param value     the string to validate
     * @param minLength the minimum expected length (inclusive, defaults to zero)
     * @param maxLength the maximum expected length (inclusive). Leave unspecified or {@code null} to allow any max length
     */
    @Operation
    public void length(final String value, @Optional(defaultValue = "0") final int minLength, @Optional final Integer maxLength) throws Exception
    {
        validateWith(new Validator()
        {
            private String error;

            @Override
            public boolean isValid()
            {
                int inputLength = value.length();
                if (inputLength < minLength)
                {
                    error = String.format("value '%s' was expected to have a length of at least %d characters but it only has %d", value, minLength, inputLength);
                    return false;
                }

                if (maxLength != null && inputLength > maxLength)
                {
                    error = String.format("value '%s' was expected to have a length of at most %d characters but it has %d", value, maxLength, inputLength);
                    return false;
                }

                return true;
            }

            @Override
            public String getErrorMessage()
            {
                return error;
            }
        });
    }

    /**
     * Receives a numeric {@code value} as a {@link String} and checks that it can be parsed as a {@link Integer}
     *
     * @param options the number options
     */
    @Operation
    public void isLong(NumberValidationOptions options) throws Exception
    {
        validateWith(new NumberValidator(options)
        {
            @Override
            protected Number validateWithPattern(String value, String pattern, Locale locale)
            {
                return LongValidator.getInstance().validate(value, pattern, locale.getJavaLocale());
            }

            @Override
            protected Number validateWithoutPattern(String value, Locale locale)
            {
                return LongValidator.getInstance().validate(value, locale.getJavaLocale());
            }

            @Override
            protected Class<? extends Number> getNumberType()
            {
                return Long.class;
            }
        });
    }

    /**
     * Validates that {@code value} is not empty. The definition of empty depends on
     * the type of {@code value}. If it's a {@link String} it will check that it is not blank.
     * If it's a {@link Collection} or {@link Map} it will check that it's not empty. No other types
     * are supported, an {@link IllegalArgumentException} will be thrown if any other type is supplied
     *
     * @param value the value to check
     * @throws IllegalArgumentException if {@code value} is something other than a {@link String},{@link Collection} or {@link Map}
     */
    @Operation
    public void notEmpty(final Object value) throws Exception
    {
        validateWith(new Validator()
                     {
                         private String error;

                         @Override
                         public boolean isValid()
                         {
                             if (value == null || value instanceof NullPayload)
                             {
                                 error = "value is null";
                                 return false;
                             }
                             else if (value instanceof Collection)
                             {
                                 if (((Collection<?>) value).isEmpty())
                                 {
                                     error = "collection is null";
                                     return false;
                                 }
                             }
                             else if (value instanceof String)
                             {
                                 if (StringUtils.isBlank((String) value))
                                 {
                                     error = "String is blank";
                                     return false;
                                 }
                             }
                             else if (value instanceof Map)
                             {
                                 if (((Map<?, ?>) value).isEmpty())
                                 {
                                     error = "map is empty";
                                     return false;
                                 }
                             }
                             else if (value instanceof BlankLiteral)
                             {
                                 error = "value is a BlankLiteral";
                                 return false;
                             }
                             else
                             {

                                 throw new IllegalArgumentException(
                                         String.format(
                                                 "Only instances of Map, Collection and String can be checked for emptyness. Instance of %s was found instead",
                                                 value.getClass().getName()));
                             }

                             return true;
                         }

                         @Override
                         public String getErrorMessage()
                         {
                             return error;
                         }
                     }
        );
    }

    /**
     * Validates that the given {@code value} is not {@code null} nor
     * an instance of {@link NullPayload}
     *
     * @param value the value to test
     */
    @Operation
    public void notNull(final Object value) throws Exception
    {
        validateWith(new Validator()
        {
            @Override
            public boolean isValid()
            {
                return value != null && !(value instanceof NullPayload);
            }

            @Override
            public String getErrorMessage()
            {
                return "The value is null";
            }
        });
    }

    /**
     * Validates that the given {@code value} is {@code null} or
     * an instance of {@link NullPayload}
     *
     * @param value the value to test
     */
    @Operation
    public void isNull(final Object value) throws Exception
    {
        validateWith(new Validator()
        {
            @Override
            public boolean isValid()
            {
                return value == null || value instanceof NullPayload;
            }

            @Override
            public String getErrorMessage()
            {
                return "The value is not null";
            }
        });
    }

    /**
     * Receives a numeric {@code value} as a {@link String} and checks that it can be parsed as a {@link Short}
     *
     * @param options the number options
     */
    @Operation
    public void isShort(NumberValidationOptions options) throws Exception
    {
        validateWith(new NumberValidator(options)
        {
            @Override
            protected Number validateWithPattern(String value, String pattern, Locale locale)
            {
                return ShortValidator.getInstance().validate(value, pattern, locale.getJavaLocale());
            }

            @Override
            protected Number validateWithoutPattern(String value, Locale locale)
            {
                return ShortValidator.getInstance().validate(value, locale.getJavaLocale());
            }

            @Override
            protected Class<? extends Number> getNumberType()
            {
                return Short.class;
            }
        });
    }

    /**
     * Checks that a {@code time} in {@link String} format is valid for the given {@code pattern} and {@code locale}.
     * If no pattern is provided, then the {@code locale}'s default will be used
     *
     * @param time    A date in String format
     * @param locale  the locale of the String
     * @param pattern the pattern for the {@code date}
     */
    @Operation
    public void time(final String time, @Optional(defaultValue = "US") final Locale locale, @Optional final String pattern) throws Exception
    {
        validateWith(new Validator()
        {
            @Override
            public boolean isValid()
            {
                TimeValidator validator = TimeValidator.getInstance();

                if (pattern != null)
                {
                    if (!validator.isValid(time, pattern, locale.getJavaLocale()))
                    {
                        return false;
                    }
                }
                else
                {
                    if (!validator.isValid(time, locale.getJavaLocale()))
                    {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public String getErrorMessage()
            {
                return String.format("%s is not a valid time for the pattern %s under locale %s", time, pattern, locale);
            }
        });
    }

    /**
     * Checks that {@code countryCode} matches any IANA-defined
     * top-level domain. Leading dots are ignored if present. The
     * search is case-sensitive.
     *
     * @param countryCode the country code to validate
     */
    @Operation
    public void domainCountry(final String countryCode) throws Exception
    {
        validateWith(new Validator()
        {

            @Override
            public boolean isValid()
            {
                return DomainValidator.getInstance().isValidCountryCodeTld(countryCode);
            }

            @Override
            public String getErrorMessage()
            {
                return countryCode + " is not a valid top level domain country code";
            }
        });
    }

    /**
     * Checks that {@code domain} matches any IANA-defined
     * top-level domain. Leading dots are ignored if present. The
     * search is case-sensitive.
     *
     * @param domain the domain to validate
     */
    @Operation
    public void topLevelDomain(final String domain) throws Exception
    {
        validateWith(new Validator()
        {

            @Override
            public boolean isValid()
            {
                return DomainValidator.getInstance().isValidTld(domain);
            }

            @Override
            public String getErrorMessage()
            {
                return domain + " is not a valid top level domain";
            }
        });
    }

    /**
     * Checks that {@code url} is a valid one
     *
     * @param url             the URL to validate as a {@link String}
     * @param allowTwoSlashes Whether to allow two slashes in the path component of the URL
     * @param allowAllSchemes Whether to allow all validly formatted schemes to pass validation
     * @param allowLocalUrls  Whether to allow local URLs, such as http://localhost/
     * @param noFragments     Enabling this options disallows any URL fragment
     */
    @Operation
    public void url(final String url,
                    final @Optional(defaultValue = "true") boolean allowTwoSlashes,
                    final @Optional(defaultValue = "true") boolean allowAllSchemes,
                    final @Optional(defaultValue = "true") boolean allowLocalUrls,
                    final @Optional(defaultValue = "false") boolean noFragments) throws Exception
    {
        validateWith(new Validator()
        {
            @Override
            public boolean isValid()
            {
                long options = 0;

                if (allowAllSchemes)
                {
                    options |= UrlValidator.ALLOW_ALL_SCHEMES;
                }
                if (allowTwoSlashes)
                {
                    options |= UrlValidator.ALLOW_2_SLASHES;
                }
                if (allowLocalUrls)
                {
                    options |= UrlValidator.ALLOW_LOCAL_URLS;
                }
                if (noFragments)
                {
                    options |= UrlValidator.NO_FRAGMENTS;
                }

                UrlValidator validator = new UrlValidator(options);

                return validator.isValid(url);
            }

            @Override
            public String getErrorMessage()
            {
                return url + " is not a valid url";
            }
        });
    }

    /**
     * Checks that {@code value} matches the {@code regex} regular expression
     *
     * @param value         the value to check
     * @param regex         the regular expression to check against
     * @param caseSensitive when {@code true} matching is case sensitive, otherwise matching is case in-sensitive
     */
    @Operation
    public void matchesRegex(final String value, final String regex, @Optional(defaultValue = "true") final boolean caseSensitive) throws Exception
    {
        validateWith(new Validator()
        {
            @Override
            public boolean isValid()
            {
                RegexValidator validator = new RegexValidator(new String[] {regex}, caseSensitive);
                return validator.isValid(value);
            }

            @Override
            public String getErrorMessage()
            {
                return String.format("Value %s is not valid under the terms of regex %s", value, regex);
            }
        });
    }
}
