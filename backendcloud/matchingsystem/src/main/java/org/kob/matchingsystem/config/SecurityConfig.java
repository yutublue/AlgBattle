package org.kob.matchingsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()//禁用CSRF保护
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)//设置无状态会话
                .and()
                .authorizeRequests()
                .antMatchers("/player/add/", "/player/remove/", "127.0.0.1").permitAll()
                .antMatchers(HttpMethod.OPTIONS).permitAll()
                .anyRequest().authenticated();//如果被拒绝会返回401
    }
}
