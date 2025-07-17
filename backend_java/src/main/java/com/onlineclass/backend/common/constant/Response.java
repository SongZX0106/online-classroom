package com.onlineclass.backend.common.constant;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author songzx
 * @date 2023/6/4
 * @apiNote
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // 值等于null的属性不返回
public class Response<T> {
    private String code;

    private String msg;

    private T data;


    /**
     * @title 成功消息
     * @return
     */
    public static <T> Response<T> success() {
        return rspMsg(ResponseEnum.SUCCESS);
    }

    /**
     * @title 失败消息
     * @return
     */
    public static <T> Response<T> error() {
        return rspMsg(ResponseEnum.SERVER_INNER_ERR);
    }

    public static <T> Response<T> error(String msg) {
        Response<T> responseData = new Response<T>();
        responseData.setCode(ResponseEnum.SERVER_INNER_ERR.getCode());
        responseData.setMsg(msg);
        return responseData;
    }

    /**
     * @title 自定义消息
     * @return
     */
    public static <T> Response<T> rspMsg(ResponseEnum responseEnum) {
        Response<T> message = new Response<T>();
        message.setCode(responseEnum.getCode());
        message.setMsg(responseEnum.getMsg());
        return message;
    }

    /**
     * @title 自定义消息
     * @return
     */
    public static <T> Response<T> rspMsg(String code , String msg) {
        Response<T> message = new Response<T>();
        message.setCode(code);
        message.setMsg(msg);
        return message;
    }

    /**
     * @title 返回数据
     * @param data
     * @return
     */
    public static <T> Response<T> rspData(T data) {
        Response<T> responseData = new Response<T>();
        responseData.setCode(ResponseEnum.SUCCESS.getCode());
        responseData.setMsg(ResponseEnum.SUCCESS.getMsg());
        responseData.setData(data);
        return responseData;
    }

    /**
     * @title 返回数据-自定义code
     * @param data
     * @return
     */
    public static <T> Response<T> rspData(String code , T data) {
        Response<T> responseData = new Response<T>();
        responseData.setCode(code);
        responseData.setData(data);
        return responseData;
    }


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
