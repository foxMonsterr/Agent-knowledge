package com.chat.myAgent.common.constant;

/**
 * 统一响应码
 *
 * 说明：
 * - 200 系列：成功
 * - 400 系列：参数/权限/业务问题
 * - 500 系列：服务端异常
 * - 503 系列：第三方模型服务异常
 */
public final class ResultCode {

    private ResultCode() {
    }

    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int RATE_LIMIT = 429;
    public static final int INTERNAL_ERROR = 500;
    public static final int THIRD_PARTY_ERROR = 503;
}
