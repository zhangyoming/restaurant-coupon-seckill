package com.zym.restaurant.coupon.common.exception;

import com.zym.restaurant.coupon.common.CommonResult;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public CommonResult<?> handleBusinessException(BusinessException ex) {
        return CommonResult.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage();
        return CommonResult.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    public CommonResult<?> handleBindException(BindException ex) {
        String message = Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage();
        return CommonResult.error(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public CommonResult<?> handleConstraintViolationException(ConstraintViolationException ex) {
        return CommonResult.error(400, ex.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public CommonResult<?> handleDuplicateKeyException(DuplicateKeyException ex) {
        return CommonResult.error(409, "数据已存在，请勿重复提交");
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<?> handleException(Exception ex) {
        ex.printStackTrace();
        return CommonResult.error(500, "系统异常：" + ex.getMessage());
    }
}
