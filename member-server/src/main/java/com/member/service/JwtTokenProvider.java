package com.member.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Component
public class JwtTokenProvider {

	private final SecretKey secretKey;
	private final long expirationTime;
	private final long refreshExpirationTime;

	public JwtTokenProvider(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.expiration}") long expirationTime,
		@Value("${jwt.refresh-expiration}") long refreshExpirationTime
	) {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 bytes)");
		}
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
		this.expirationTime = expirationTime;
		this.refreshExpirationTime = refreshExpirationTime;
	}

	/**
	 * Access Token 생성
	 */
	public String generateAccessToken(Long userId, String email, String role) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expirationTime);

		return Jwts.builder()
			.subject(String.valueOf(userId))
			.claim("email", email)
			.claim("role", role)
			.claim("type", "access")
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
	}

	/**
	 * Refresh Token 생성
	 */
	public String generateRefreshToken(Long userId) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + refreshExpirationTime);

		return Jwts.builder()
			.subject(String.valueOf(userId))
			.claim("type", "refresh")
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
	}

}
