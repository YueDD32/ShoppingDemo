package com.leyou.auth.service;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.user.client.UserClient;
import com.leyou.user.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private JwtProperties prop;

    @Autowired
    private UserClient userClient;

    public String login(String username, String password) {
        try {
            // 查询用户
            User user = userClient.queryByUsernameAndPassword(username, password);
            if (user == null) {
                // 登录失败
                throw new LyException(ExceptionEnum.INVALID_UN_PW_ERROR);
            }
            // 封装载荷信息
            UserInfo userInfo = new UserInfo(user.getId(), user.getUsername());
            // 生成token
            String token = JwtUtils.generateToken(userInfo, prop.getPrivateKey(), prop.getExpire());
            return token;
        }catch (Exception e){
            log.error("【授权中心】用户{}登录异常!", username, e);
            throw new LyException(ExceptionEnum.INVALID_UN_PW_ERROR);
        }
    }
}
