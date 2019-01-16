package com.youtube.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.Version;

@Configuration
/**
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * @author saibaba
 *
 */
public class FacebookConfig {

	private static final String FB_USER = "xxxxxxx";

	private static final String FB_PWD = "xxxxxx";

	@Bean
	public AccessToken fbAccessToken() {
		AccessToken accessToken = new DefaultFacebookClient(Version.LATEST).obtainAppAccessToken(FB_USER, FB_PWD);
		System.out.println(":::::::::: My FB access token :::::::::::::" + accessToken);
		return accessToken;
	}

}
