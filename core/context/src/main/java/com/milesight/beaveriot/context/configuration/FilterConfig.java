package com.milesight.beaveriot.context.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.authentication.facade.IAuthenticationFacade;
import com.milesight.beaveriot.context.filter.HttpRequestFilter;
import com.milesight.beaveriot.context.filter.QueryParameterNameConversionFilter;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public FilterRegistrationBean<HttpRequestFilter> authenticationFilter() {
        FilterRegistrationBean<HttpRequestFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new HttpRequestFilter(authenticationFacade));
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<QueryParameterNameConversionFilter> queryParameterNameConversionFilter() {
        FilterRegistrationBean<QueryParameterNameConversionFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new QueryParameterNameConversionFilter(objectMapper));
        registrationBean.addUrlPatterns("*");
        return registrationBean;
    }

}
