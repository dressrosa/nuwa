package com.xiaoyu.nuwa.utils.enums;

import lombok.Getter;

@Getter
public enum RequestHeaderEnum {

    TRACEID("traceid"),

    SPANID("spanid"),

    XREQUESTID("x-request-id"),

    ROLE("role"),

    /**
     * session id
     */
    SID("sid"),

    SIGN("sign"),

    USERID("userId"),

    HOST("host"),

    ;

    String name;

    RequestHeaderEnum(String name) {
        this.name = name;
    }

}
