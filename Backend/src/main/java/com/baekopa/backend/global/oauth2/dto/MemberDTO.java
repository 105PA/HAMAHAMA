package com.baekopa.backend.global.oauth2.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberDTO {
    private String role;
    private String name;
    private String providerCode;
}
