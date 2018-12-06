package com.leyou.auth.web;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.service.AuthService;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.CookieUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@EnableConfigurationProperties(JwtProperties.class)
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProperties properties;

    /**
     * 登录方法
     *
     * @param username
     * @param password
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestParam("username") String username, @RequestParam("password") String password,
            HttpServletResponse response, HttpServletRequest request) {
        // 登录校验
        String token = authService.login(username, password);
        // 写入cookie中
        CookieUtils.newBuilder(response)
                .httpOnly().request(request).build(properties.getCookieName(), token);
        return ResponseEntity.ok().build();
    }

    /**
     * 校验用户登录状态
     *
     * @param token
     * @return
     */
    @GetMapping("verify")
    public ResponseEntity<UserInfo> verify(
            @CookieValue("LY_TOKEN") String token,
            HttpServletResponse response, HttpServletRequest request) {
        try {
            // 校验和解析jwt
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, properties.getPublicKey());

            // 刷新token
            token = JwtUtils.generateToken(userInfo, properties.getPrivateKey(), properties.getExpire());
            // 写入cookie
            CookieUtils.newBuilder(response)
                    .httpOnly().request(request).build(properties.getCookieName(), token);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            // 校验失败，返回401，代表未授权！
            throw new LyException(ExceptionEnum.UNAUTHORIZED);
        }
    }
}
