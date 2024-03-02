package iq.earthlink.social.common.config;

import springfox.documentation.spring.web.plugins.Docket;

/**
 * The interface allows customizing Swagger Docket instance.
 * Should be registered as a bean.
 */
@FunctionalInterface
public interface SwaggerConfigCustomizer {

  Docket customize(Docket docket);
}
