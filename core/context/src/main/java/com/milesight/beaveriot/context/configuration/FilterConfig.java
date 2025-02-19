package com.milesight.beaveriot.context.configuration;

import com.milesight.beaveriot.authentication.facade.IAuthenticationFacade;
import com.milesight.beaveriot.context.filter.HttpRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author loong
 */
@Configuration
public class FilterConfig {

    @Autowired
    private IAuthenticationFacade authenticationFacade;

    @Bean
    public FilterRegistrationBean<HttpRequestFilter> customFilter() {
        FilterRegistrationBean<HttpRequestFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new HttpRequestFilter(authenticationFacade));
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }

}
