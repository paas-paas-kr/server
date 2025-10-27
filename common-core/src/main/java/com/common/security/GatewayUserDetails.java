package com.common.security;

import com.common.enumtype.Language;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

/**
 * Gateway에서 전달받은 사용자 정보를 담는 클래스
 */
@Getter
@AllArgsConstructor
public class GatewayUserDetails implements Serializable {

    private final Long userId;
    private final String email;
    private final Language preferredLanguage;

    @Override
    public String toString() {
        return "User{id=" + userId + ", email=" + email + ", preferredLanguage=" + preferredLanguage + "}";
    }
}
