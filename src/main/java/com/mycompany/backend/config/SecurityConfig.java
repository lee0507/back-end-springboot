package com.mycompany.backend.config;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.mycompany.backend.security.JwtAuthenticationFilter;

import lombok.extern.log4j.Log4j2;

@Log4j2
@EnableWebSecurity  //웹 시큐리티를 활성화하겠다는 어노테이션
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  
  @Resource
  private RedisTemplate redisTemplate;
  
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    log.info("실행");
    //서버 세션 비활성화(JSESSION 아이디가 생성되지 않는다.)
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    
    //폼 로그인 비활성화(MPA 방식에서는 disable하면 안됨)
    http.formLogin().disable();
    
    //사이트간 요청 위조 방지 비활성화
    http.csrf().disable();
    
    //요청 경로 권한 설정
    http.authorizeRequests()
    .antMatchers("/board/**").authenticated() //(board 밑의 요청은 인증이 필요하다.)
    .antMatchers("/**").permitAll();  //나머지는 전부 허가하겠다.(Spring security가 관여를 해서 나머지는 전부 허가)
    
    //CORS 설정(다른 도메인의 JavaScript로 접근을 할 수 있도록 허용)(디테일 설정은 따로 아래에서 한다)
    http.cors();
    
    //JWT 인증 필터 추가
    http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class); 
    //UsernamePasswordAuthenticationFilter 필터는 MPA인증 방식에서 name이랑 password로 DB에서 인증하는 방식(폼 로그인)
  }
  
  public JwtAuthenticationFilter jwtAuthenticationFilter() {  //다른데서 사용하지 않기 때문에 Bean을 붙여서 관리객체로 만들 필요가 없음
    JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter();
    jwtAuthenticationFilter.setRedisTemplate(redisTemplate);
    return jwtAuthenticationFilter;
  }
  
  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {  //데이터베이스 인증
    log.info("실행");
    
    //폼 인증 방식에서 사용(MPA 방식으로 개발할 때 설정)(JWT 인증 방식에서는 사용하지 않음)
    /*    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(new CustomUserDetailsService()); //사용자에 대한 상세서비스를 제공(DB에 있는 내용을 말함)
    provider.setPasswordEncoder(passwordEncoder());
    auth.authenticationProvider(provider);*/
      
  }
  
  @Override
  public void configure(WebSecurity web) throws Exception { //인증이 필요없는 경로 설정
    log.info("실행");
    DefaultWebSecurityExpressionHandler defaultWebSecurityExpressionHandler = new DefaultWebSecurityExpressionHandler();
    defaultWebSecurityExpressionHandler.setRoleHierarchy(roleHierarchyImpl());//관리객체로 등록된 것들은 실행한다고 하기 보단 가져다 쓴다.(함수 호출되는 게 아님)
    web.expressionHandler(defaultWebSecurityExpressionHandler);
    
    //MPA방식에서 시큐리티를 적용하지 않는 경로 설정
    /*    web.ignoring() //(Spring security가 관여를 아예 하지 않음, 보안 신경 안쓰는 부분)
    .antMatchers("/images/**")
    .antMatchers("/css/**")
    .antMatchers("/js/**")
    .antMatchers("/bootstrap/**")
    .antMatchers("/jquery/**")
    .antMatchers("/favicon.ico"); */
  }
  
  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();//델리게이팅 패스워드 인코더
//    return new BCryptPasswordEncoder(); //Bcrypt로만 암호화
  }
  
  @Bean
  public RoleHierarchyImpl roleHierarchyImpl() {  
     log.info("실행");
     RoleHierarchyImpl roleHierarchyImpl = new RoleHierarchyImpl();
     roleHierarchyImpl.setHierarchy("ROLE_ADMIN > ROLE_MANAGER > ROLE_USER");
     return roleHierarchyImpl;
  }
  
  //Rest API에서만 사용
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    //CORS를 활성화하면 이 객체가 있는 지 확인을 하고 있으면 내부에 적힌 정보들로 허용할지 말지 설정
    log.info("실행");
      CorsConfiguration configuration = new CorsConfiguration();
      //모든 요청 사이트 허용
      configuration.addAllowedOrigin("*");  //모든 도메인의 자바스크립트를 허용
      //모든 요청 방식 허용
      configuration.addAllowedMethod("*");
      //모든 요청 헤더 허용
      configuration.addAllowedHeader("*");
      //모든 URL 요청에 대해서 위 내용을 적용
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", configuration);
      return source;
  }
}
