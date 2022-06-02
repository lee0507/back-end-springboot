package com.mycompany.backend.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycompany.backend.dto.Member;
import com.mycompany.backend.security.Jwt;
import com.mycompany.backend.service.MemberService;
import com.mycompany.backend.service.MemberService.JoinResult;
import com.mycompany.backend.service.MemberService.LoginResult;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/member")
public class MemberController {

  @Resource
  private MemberService memberService;
  
  @Resource
  private PasswordEncoder passwordEncoder;
  
  @Resource
  private RedisTemplate<String, String> redisTemplate;
  
  @PostMapping("/join")
  public Map<String, Object> join(@RequestBody Member member) {
    
    //계정 활성화(클라이언트에서는 enable이 세팅이 되지 않은 상태로 넘어옴
    member.setMenabled(true);
    
    //패스워드 암호화
    member.setMpassword(passwordEncoder.encode(member.getMpassword()));
    
    //회원가입 처리
    JoinResult joinResult = memberService.join(member);
    
    //응답내용 설정
    Map<String, Object> map = new HashMap<>();
    if(joinResult == JoinResult.SUCCESS) {
      map.put("result", "success");
    } else if(joinResult == JoinResult.DUPLICATED) {
      map.put("result", "duplicated");
    } else {
      map.put("result", "fail");
    }    
    return map;
  }
  
  @PostMapping("/login")
  public ResponseEntity<String> login(@RequestBody Member member) {
    
    //mid와 mpassword가 없을 경우(클라이언트에서 보내온 정보가 비어있을 경우)
    if(member.getMid() == null || member.getMpassword() == null) {
      return ResponseEntity.status(401).body("mid or mpassword cannot be null");
    }
    
    //로그인 결과 얻기
    LoginResult loginResult = memberService.login(member);
    
    //로그인 정보가 정확하지 않은 경우(에러코드와 메세지를 리턴한다고 생각하면, 클라이언트에선 catch쪽에서 확인할 수 있음)
    if(loginResult != LoginResult.SUCCESS) {
      return ResponseEntity.status(401).body("mid or mpassword is wrong");
    }
    
    //로그인 정보가 정확하면 클라이언트에게 보낼 토큰을 생성한다.
    Member dbMember = memberService.getMember(member.getMid());
    String accessToken = Jwt.createAccessToken(member.getMid(), dbMember.getMrole());
    String refreshToken = Jwt.createRefreshToken(member.getMid(), dbMember.getMrole());
    
    //Redis에 저장
    ValueOperations<String, String> vo = redisTemplate.opsForValue();
    vo.set(accessToken, refreshToken, Jwt.REFRESH_TOKEN_DURATION, TimeUnit.MICROSECONDS); //(키, 값, 유효시간, 시간단위)
    
    
    //쿠키 생성(refreshToken은 쿠키에 들어가도록 설정)
    String refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                                              .httpOnly(true) //클라이언트 자바스크립트에서 읽을 수 없도록 하는 것
                                              .secure(false)  //true면 https만 가능 false면 http, https 둘다 가능
                                              .path("/")      //전체 RestAPI 요청 설정
                                              .maxAge(Jwt.REFRESH_TOKEN_DURATION / 1000) //초단위로 값을 넣어야해서 밀리세컨 나누기 1000
                                              .domain("localhost")
                                              .build()
                                              .toString()
                                              ;
    
    //본문 생성
    String json = new JSONObject()
                        .put("accessToken", accessToken)
                        .put("mid", member.getMid())
                        .toString();
    
    //응답 설정
    return ResponseEntity
                //응답상태 코드: 200
                .ok()  
                //응답 헤더 추가
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                //응답 바디 추가
                .body(json);
    
  }
  
  @GetMapping("/refreshToken")  //만료된 accessToken이 내가 마지막으로 보낸 토큰인지만 검사한다. refreshToken은 내가 보낸 건지, 만료된 건지 확인
  public ResponseEntity<String> refreshToken(
      @RequestHeader("Authorization") String authorization,
      @CookieValue("refreshToken") String refreshToken) {
    //accessToken은 헤더에서 가져오고 refreshToken은 쿠키에서 가져온다.
    
    //AccessToken 얻기
    String accessToken = Jwt.getAccessToken(authorization); //[Authorization] 헤더에서 Bearer부분을 잘라내는 코드
    
    //AccessToken이 헤더에 있는지 없는지 확인
    if(accessToken == null) {
      return ResponseEntity.status(401).body("no access token");
    }
    
    //RefreshToken이 쿠키에 있는지 없는지 확인
    if(refreshToken == null) {
      return ResponseEntity.status(401).body("no refresh token");
    }
    
    //동일한 토큰인지 레디스에서 확인(레디스에는 키가 accessToken, 값이 refreshToken)
    ValueOperations<String, String> vo = redisTemplate.opsForValue();
    String redisRefreshToken = vo.get(accessToken);
    
    //accessToken이 없는 경우(레디스에 키가 없는 경우)
    if(redisRefreshToken == null) {
      return ResponseEntity.status(401).body("invalidate access token");
    }
    
    //클라이언트가 보낸 refreshToken이 redis에 저장된 refreshToken과 일치하는 지 확인
    if(!refreshToken.equals(redisRefreshToken)) {
      return ResponseEntity.status(401).body("invalidate refresh token");
    }
    
    
    //새로운 AccessToken 생성
    Map<String, String> userInfo = Jwt.getUserInfo(refreshToken); //refreshToken에도 mid와 authority를 저장해두었기 때문에 여기서 정보를 꺼내와서 토큰 생성한다.
    String mid = userInfo.get("mid");
    String authority = userInfo.get("authority");
    String newAccessToken = Jwt.createAccessToken(mid, authority);
    
    //Redis에 저장된 기존 정보를 삭제(키인 accessToken이 변경되었기 때문에 새로 저장해주어야 함)
    redisTemplate.delete(accessToken);
    
    //Redis에 새로운 정보를 저장
    Date expiration = Jwt.getExpiration(refreshToken);  //refreshToken의 남은 기간을 가져온다.
    vo.set(newAccessToken, refreshToken, expiration.getTime() - new Date().getTime(), TimeUnit.MILLISECONDS); //refreshToken의 남은 시간을 만료기한으로 넣어줌
    
    //응답 바디에 넣을 json 설정
    String json = new JSONObject()
                      .put("accessToken", newAccessToken)
                      .put("mid", mid)
                      .toString();
    
    return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(json);
  }
  
  @GetMapping("/logout")
  public ResponseEntity<String> logout(@RequestHeader("Authorization") String authorization) {
    String accesstoken = Jwt.getAccessToken(authorization);
    if(accesstoken == null) {
      return ResponseEntity.status(401).body("invalidate access token");
    }
    
    //Redis에 저장된 인증 정보 삭제
    redisTemplate.delete(accesstoken);
    
    //RefreshToken 쿠키 삭제(쿠키의 maxAge를 0으로 설정하면 삭제되는 효과)
    String refreshTokenCookie = ResponseCookie.from("refreshToken", "")
        .httpOnly(true) //클라이언트 자바스크립트에서 읽을 수 없도록 하는 것
        .secure(false)  //true면 https만 가능 false면 http, https 둘다 가능
        .path("/")      //전체 RestAPI 요청 설정
        .maxAge(0) //초단위로 값을 넣어야해서 밀리세컨 나누기 1000
        .domain("localhost")
        .build()
        .toString()
        ;
    
    //응답 설정
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshTokenCookie).body("success");
  }
}
