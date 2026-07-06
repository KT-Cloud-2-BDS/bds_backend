package com.bds.support;


import com.bds.order.infrastructure.security.GatewayAuthFilter;
import com.bds.order.infrastructure.security.SecurityConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;


@Import({SecurityConfig.class, GatewayAuthFilter.class})
public abstract class MockMvcTestSupport {

    protected MockMvc mockMvc;
    
    @Autowired
    private WebApplicationContext context;

    @PostConstruct
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }
}
