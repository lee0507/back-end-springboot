package com.mycompany.backend.security;

import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/*
인증이 성공되고 나서 인증된 정보를 얻을 때 이 객체를 얻을 수 있다.
서버쪽에서 인증된 정보를 얻을 때 사용


*/
public class CustomUserDetails extends User {
  //인증 정보로 추가로 저장하고 싶은 내용 정의
	private String mname;
	private String memail;
	
	public CustomUserDetails(
			String mid, 
			String mpassword, 
			boolean menabled, 
			List<GrantedAuthority> mauthorities,
			//위의 네 가지 정보는 security의 User가 무조건 필요로 하기 때문
			String mname,
			String memail) {
		super(mid, mpassword, menabled, true, true, true, mauthorities);
		this.mname = mname;
		this.memail = memail;
	}
	
	public String getMname() {
		return mname;
	}

	public String getMemail() {
		return memail;
	}
}

