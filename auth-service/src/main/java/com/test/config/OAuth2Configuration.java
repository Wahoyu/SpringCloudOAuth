package com.test.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import javax.annotation.Resource;

@EnableAuthorizationServer   //开启验证服务器
@Configuration
public class OAuth2Configuration extends AuthorizationServerConfigurerAdapter {

    @Resource
    private AuthenticationManager manager;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 这个方法是对客户端进行配置，一个验证服务器可以预设很多个客户端，
     * 之后这些指定的客户端就可以按照下面指定的方式进行验证
     * @param clients 客户端配置工具
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients
                .inMemory()
                .withClient("web")
                .secret(encoder.encode("654321"))
                .autoApprove(false)   //这里把自动审批开了，就不用再去手动选同意了
                .scopes("book", "user", "borrow")
                .redirectUris("http://localhost:8101/login", "http://localhost:8201/login", "http://localhost:8301/login")
                .authorizedGrantTypes("client_credentials", "password", "implicit", "authorization_code", "refresh_token");
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) {
        security
                .passwordEncoder(encoder)    //编码器设定为BCryptPasswordEncoder
                .allowFormAuthenticationForClients()  //允许客户端使用表单验证，一会我们POST请求中会携带表单信息
                .checkTokenAccess("permitAll()");     //允许所有的Token查询请求
    }

    @Resource
    TokenStore store;

    @Resource
    JwtAccessTokenConverter converter;

    @Resource
    UserDetailsService service;

    private AuthorizationServerTokenServices serverTokenServices(){  //这里对AuthorizationServerTokenServices进行一下配置
        DefaultTokenServices services = new DefaultTokenServices();
        services.setSupportRefreshToken(true);   //允许Token刷新
        services.setTokenStore(store);   //添加刚刚的TokenStore
        services.setTokenEnhancer(converter);   //添加Token增强，其实就是JwtAccessTokenConverter，增强是添加一些自定义的数据到JWT中
        return services;
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        endpoints
                .tokenServices(serverTokenServices())   //设定为刚刚配置好的AuthorizationServerTokenServices
                .userDetailsService(service)
                .authenticationManager(manager);
    }


}