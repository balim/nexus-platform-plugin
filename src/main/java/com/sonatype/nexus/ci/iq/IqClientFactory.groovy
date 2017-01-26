/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import javax.annotation.Nullable

import com.sonatype.nexus.api.common.Authentication
import com.sonatype.nexus.api.common.ServerConfig
import com.sonatype.nexus.api.iq.internal.InternalIqClient
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder
import com.sonatype.nexus.ci.config.NxiqConfiguration

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder
import hudson.model.ItemGroup
import hudson.security.ACL
import org.slf4j.Logger

class IqClientFactory
{
  static InternalIqClient getIqClient() {
    return getIqClient(NxiqConfiguration.serverUrl, NxiqConfiguration.credentialsId)
  }

  static InternalIqClient getIqClient(URI serverUrl, @Nullable String credentialsId) {
    //TODO probably need to add proxy support
    return (InternalIqClient) InternalIqClientBuilder.create()
        .withServerConfig(getServerConfig(serverUrl, credentialsId))
        .build()
  }

  static InternalIqClient getIqClient(Logger log, @Nullable String credentialsId) {
    //TODO probably need to add proxy support
    return (InternalIqClient) InternalIqClientBuilder.create()
        .withServerConfig(getServerConfig(NxiqConfiguration.serverUrl, credentialsId ?: NxiqConfiguration.credentialsId))
        .withLogger(log)
        .build()
  }

  static InternalIqClient getIqClient(URI serverUrl, Logger log) {
    //TODO probably need to add proxy support
    return (InternalIqClient) InternalIqClientBuilder.create()
        .withServerConfig(new ServerConfig(serverUrl))
        .withLogger(log)
        .build()
  }

  static ServerConfig getServerConfig(URI url, @Nullable String credentialsId) {
    if (credentialsId) {
      def authentication = loadCredentials(url, credentialsId)
      return new ServerConfig(url, authentication)
    }
    else {
      return new ServerConfig(url)
    }
  }

  static private Authentication loadCredentials(final URI url, final String credentialsId) {
    def lookupCredentials = CredentialsProvider.lookupCredentials(
        StandardUsernamePasswordCredentials.class,
        (ItemGroup) jenkins.model.Jenkins.getInstance(),
        ACL.SYSTEM,
        URIRequirementBuilder.fromUri(url.toString()).build())

    def credentials = CredentialsMatchers.firstOrNull(lookupCredentials, CredentialsMatchers.withId(credentialsId))
    if (!credentials) {
      throw new IllegalArgumentException("No credentials were found for credentialsId: ${credentialsId}")
    }

    return new Authentication(credentials.getUsername(), credentials.getPassword().getPlainText())
  }
}