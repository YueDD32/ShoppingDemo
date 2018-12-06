package com.leyou.cart.interceptors;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.cart.config.JwtProperties;
import com.leyou.common.utils.CookieUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用户信息拦截器
 */
@Slf4j
public class UserInterceptor implements HandlerInterceptor {

    private JwtProperties prop;

    public UserInterceptor(JwtProperties prop) {
        this.prop = prop;
    }

    private static final ThreadLocal<UserInfo> TL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // 获取token
            String token = CookieUtils.getCookieValue(request, prop.getCookieName());
            // 解析
            UserInfo user = JwtUtils.getInfoFromToken(token, prop.getPublicKey());

            // 保存用户
            TL.set(user);
            // 放行
            return true;
        }catch (Exception e){
            log.error("【购物车服务】用户登录信息无效.", e);
            // 拦截
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TL.remove();
    }

    public static UserInfo getUser(){
        return TL.get();
    }

}
