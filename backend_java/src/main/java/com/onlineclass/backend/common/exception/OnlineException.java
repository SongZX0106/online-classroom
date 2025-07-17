package com.onlineclass.backend.common.exception;

import com.onlineclass.backend.common.constant.ResponseEnum;

/**
 * program bdyth
 * description  运行时异常
 * @author sunyuyu
 * date 2022/09/29
 */
@SuppressWarnings("unused")
public class OnlineException extends RuntimeException{
    /**
     * 错误代码
     */
    private final String code;
    /**
     * 错误信息
     */
    private final String message;

    @Override
    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public OnlineException(ResponseEnum errorCode, String message) {
        this.code = errorCode.getCode();
        this.message = message;
    }

    public OnlineException(ResponseEnum errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMsg();
    }

    public OnlineException(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
