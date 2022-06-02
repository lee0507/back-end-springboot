package com.mycompany.backend.config;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Configuration
public class RedisConfig {
  
  @Value("${spring.redis.hostName}")
  private String hostName;
  
  @Value("${spring.redis.port}")
  private String port;
  
  @Value("${spring.redis.password}")
  private String password;
  
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    log.info("실행");
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    config.setHostName("localhost");
    config.setPort(6379);
    config.setPassword("redis");
    LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config);  //redis 연결 클라이언트 Lettuce
    return connectionFactory;
  }
  
  @Bean
  public RedisTemplate<String, String> redisTemplate() {
    log.info("실행");
    RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory()); //관리 객체로 등록된 것은 그냥 함수형태로 가져다가 써도 무방
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new StringRedisSerializer());  //네트워크에서 사용하기 위해 바이트 배열로 변경하기 위한 new StringRedisSerializer()
    return redisTemplate;
  }
}
