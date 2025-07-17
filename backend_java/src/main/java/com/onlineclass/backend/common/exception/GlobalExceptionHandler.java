package com.onlineclass.backend.common.exception;

import com.onlineclass.backend.common.constant.Response;
import com.onlineclass.backend.common.constant.ResponseEnum;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartException;



@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)

    public Response<Void> errorHandler(Exception exception) {

        if (exception instanceof IllegalArgumentException
                || exception instanceof ServletRequestBindingException
                || exception instanceof HttpRequestMethodNotSupportedException
                || exception instanceof HttpMediaTypeException
                || exception instanceof BindException
                || exception instanceof MultipartException
        ) {
            return Response.rspMsg(ResponseEnum.PARAM_LACK);
        }
        return Response.error();
    }
}