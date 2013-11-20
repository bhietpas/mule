/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import static org.mule.util.SplashScreen.miniSplash;

import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.launcher.DeploymentInitException;
import org.mule.module.launcher.DeploymentListener;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.DeploymentStopException;
import org.mule.module.launcher.MuleDeploymentService;
import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.module.launcher.application.MuleContextDelegateWrapper;
import org.mule.module.launcher.application.NullDeploymentListener;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.module.launcher.artifact.MuleContextDeploymentListener;
import org.mule.util.ClassUtils;
import org.mule.util.ExceptionUtils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultMuleDomain implements Domain
{
    protected transient final Log logger = LogFactory.getLog(getClass());
    protected transient final Log deployLogger = LogFactory.getLog(MuleDeploymentService.class);

    private final DomainClassLoaderFactory domainClassLoaderFactory;
    private final String domain;
    private final String domainConfigFileLocation = "mule-domain-config.xml";
    private MuleContext muleContext;
    private DeploymentListener deploymentListener;
    private ArtifactClassLoader deploymentClassLoader;

    private File configResourceFile;

    public DefaultMuleDomain(DomainClassLoaderFactory domainClassLoaderFactory, String domain)
    {
        this.domainClassLoaderFactory = domainClassLoaderFactory;
        this.deploymentListener = new NullDeploymentListener();
        this.domain = domain;
    }

    public void setDeploymentListener(DeploymentListener deploymentListener)
    {
        this.deploymentListener = deploymentListener;
    }

    public String getName()
    {
        return domain;
    }

    @Override
    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public void install()
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("New domain '%s'", getArtifactName())));
        }
        deploymentClassLoader = domainClassLoaderFactory.create(domain);
    }


    @Override
    public void init()
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("Initializing domain '%s'", getArtifactName())));
        }

        try
        {
            URL resource = deploymentClassLoader.getClassLoader().getResource(this.domainConfigFileLocation);
            if (resource != null)
            {
                this.configResourceFile = new File(resource.getFile());

                ConfigurationBuilder cfgBuilder = createConfigurationBuilder();
                if (!cfgBuilder.isConfigured())
                {
                    List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>(3);

                    // We need to add this builder before spring so that we can use Mule annotations in Spring or any other builder
                    addAnnotationsConfigBuilderIfPresent(builders);

                    builders.add(cfgBuilder);

                    DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
                    if (deploymentListener != null)
                    {
                        muleContextFactory.addListener(new MuleContextDeploymentListener(getArtifactName(), deploymentListener));
                    }
                    DomainMuleContextBuilder domainMuleContextBuilder = new DomainMuleContextBuilder(domain);
                    muleContextFactory.createMuleContext(builders, domainMuleContextBuilder);
                    this.muleContext = domainMuleContextBuilder.getMuleRealContext();
                }
            }
        }
        catch (Exception e)
        {
            // log it here so it ends up in app log, sys log will only log a message without stacktrace
            logger.error(null, ExceptionUtils.getRootCause(e));
            throw new DeploymentInitException(CoreMessages.createStaticMessage(ExceptionUtils.getRootCauseMessage(e)), e);
        }
    }

    protected void addAnnotationsConfigBuilderIfPresent(List<ConfigurationBuilder> builders) throws Exception
    {
        // If the annotations module is on the classpath, add the annotations config builder to
        // the list. This will enable annotations config for this instance.
        if (ClassUtils.isClassOnPath(MuleServer.CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, getClass()))
        {
            Object configBuilder = ClassUtils.instanciateClass(
                    MuleServer.CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, ClassUtils.NO_ARGS, getClass());
            builders.add((ConfigurationBuilder) configBuilder);
        }
    }

    private ConfigurationBuilder createConfigurationBuilder()
    {
        try
        {
            return (ConfigurationBuilder) ClassUtils.instanciateClass("org.mule.config.spring.SpringXmlDomainConfigurationBuilder",
                                                                      new Object[] {getConfigResourcesFile()[0].getName()}, deploymentClassLoader.getClassLoader());
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public void start()
    {
        try
        {
            if (this.muleContext != null)
            {
                try
                {
                    this.muleContext.start();
                }
                catch (MuleException e)
                {
                    logger.error(null, ExceptionUtils.getRootCause(e));
                    throw new DeploymentStartException(CoreMessages.createStaticMessage(ExceptionUtils.getRootCauseMessage(e)), e);
                }
            }
            // null CCL ensures we log at 'system' level
            // TODO create a more usable wrapper for any logger to be logged at sys level
            final ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            try
            {
                Thread.currentThread().setContextClassLoader(null);
                deployLogger.info(miniSplash(String.format("Started domain '%s'", getArtifactName())));
            }
            finally
            {
                Thread.currentThread().setContextClassLoader(oldCl);
            }
        }
        catch (Exception e)
        {
            throw new DeploymentStartException(CoreMessages.createStaticMessage("Failure trying to start domain " + getArtifactName()), e);
        }
    }

    @Override
    public void stop()
    {
        try
        {
            if (logger.isInfoEnabled())
            {
                logger.info(miniSplash(String.format("Stopping domain '%s'", getArtifactName())));
            }
            if (this.muleContext != null)
            {
                this.muleContext.stop();
            }
        }
        catch (Exception e)
        {
            throw new DeploymentStopException(CoreMessages.createStaticMessage("Failure trying to stop domain " + getArtifactName()), e);
        }
    }

    @Override
    public void dispose()
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("Disposing domain '%s'", getArtifactName())));
        }
        if (this.muleContext != null)
        {
            this.muleContext.dispose();
        }
    }

    @Override
    public void redeploy()
    {
    }

    @Override
    public String getArtifactName()
    {
        return domain;
    }

    @Override
    public File[] getConfigResourcesFile()
    {
        return new File[]{configResourceFile};
    }

    @Override
    public ArtifactClassLoader getArtifactClassLoader()
    {
        return deploymentClassLoader;
    }

    public void initialise()
    {
        try
        {
            if (this.muleContext != null)
            {
                this.muleContext.initialise();
            }
        }
        catch (InitialisationException e)
        {
            throw new DeploymentInitException(CoreMessages.createStaticMessage("Failure trying to initialise domain " + getArtifactName()), e);
        }
    }

    public boolean containsSharedResources()
    {
        return this.muleContext != null;
    }
}
