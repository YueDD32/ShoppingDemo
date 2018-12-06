package com.leyou.common.advice;

import com.leyou.common.exceptions.LyException;
import com.leyou.common.vo.ExceptionVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @ControllerAdvice 声明当前这个类就是一个对Controller进行拦截的通知
 */
@ControllerAdvice
public class ExceptionAdvice {

    /**
     * 注解@ExceptionHandler(RuntimeException.class) 声明当前这个方法要处理的异常的类型
     * @param e 是拦截controller以后，接收抛出的具体异常
     * @return
     */
    @ExceptionHandler(LyException.class)
    public ResponseEntity<ExceptionVO> handleException(LyException e){
        // 封装返回结果
        ExceptionVO vo = new ExceptionVO(e.getEm());
        // 返回状态码及结果
        return ResponseEntity.status(e.getStatus()).body(vo);
    }
}
