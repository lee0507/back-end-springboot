package com.mycompany.backend.security;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class JwtAuthenticationFilter extends OncePerRequestFilter { //요청 하나당 딱 한번만 실행하는 필터(OncePerRequestFilter)
  
  private RedisTemplate redisTemplate;  //관리객체만 주입을 받을 수 있음(관리 객체가 아니면 @Resource 사용불가)
  
  public void setRedisTemplate(RedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }


  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    //여기서 토큰 가져오고, 토큰 유효한지 확인하고, 마지막으로 로그인 인증처리 해야함
    log.info("실행");
    
    //요청 헤더로부터 Authorization 헤더 값 얻기
    String authorization = request.getHeader("Authorization");
    
    //AccessToken 추출
    String accessToken = Jwt.getAccessToken(authorization); //Bearer만 삭제
    
    
    //검증 작업
    if(accessToken != null && Jwt.validateToken(accessToken) ) {
      //Redis에 존재 여부 확인(accessToken이 넘어왔지만 레디스에 없다면 존재하지 않는 키)
      ValueOperations<String, String> vo = redisTemplate.opsForValue();
      String redisRefreshToken = vo.get(accessToken);
      
      if(redisRefreshToken != null) {
        //인증 처리
        Map<String, String> userInfo = Jwt.getUserInfo(accessToken);
        String mid = userInfo.get("mid");
        String authority = userInfo.get("authority");
        
        //인증 객체(이 객체가 있으면 인증이 된 것, 없으면 인증이 안된 것)
        UsernamePasswordAuthenticationToken authentication 
          = new UsernamePasswordAuthenticationToken(mid, null, AuthorityUtils.createAuthorityList(authority));
        
        //시큐리티 실행환경 객체
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);
      }
    };
    
    filterChain.doFilter(request, response); //맨 마지막 필터면 상관 없지만, 뒤에 다른 필터가 있다면 다음 필터를 실행하라고 하는 함수
  }
}
