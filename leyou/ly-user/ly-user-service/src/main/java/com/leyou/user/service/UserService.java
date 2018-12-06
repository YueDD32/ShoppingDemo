package com.leyou.user.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "user:verify:phone:";

    public Boolean checkData(String data, Integer type) {
        // 查询条件
        User r = new User();

        switch (type) {
            case 1:
                r.setUsername(data);
                break;
            case 2:
                r.setPhone(data);
                break;
            default:
                throw new LyException(ExceptionEnum.INVALID_PARAM_TYPE);
        }

        // 查询数据
        int count = userMapper.selectCount(r);
        // 判断是否为空
        return count == 0;
    }

    public void sendCode(String phone) {
        // 校验手机号
        if (!phone.matches("^1[35678]\\d{9}$")) {
            throw new LyException(ExceptionEnum.INVALID_PARAM_TYPE);
        }

        // 生成验证码
        String code = NumberUtils.generateCode(6);

        // 发送短信
        Map<String,String> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        amqpTemplate.convertAndSend("ly.sms.exchange", "sms.verify.code", msg);

        // 保存验证码
        redisTemplate.opsForValue().set(KEY_PREFIX + phone, code, 5, TimeUnit.MINUTES);
    }

    @Transactional
    public void register(User user, String code) {
        String key = KEY_PREFIX + user.getPhone();
        // 读取Redis中的验证码
        String cacheCode = redisTemplate.opsForValue().get(key);
        // 验证手机验证码
        if(!code.equals(cacheCode)){
            throw new LyException(ExceptionEnum.INVALID_PHONE_CODE);
        }

        // 获取盐
        String salt = CodecUtils.generateSalt();
        // 对密码加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));

        // 注册用户
        user.setCreated(new Date());
        user.setSalt(salt);
        userMapper.insertSelective(user);
        // 删除cacheCode
        redisTemplate.delete(key);

    }

    public User queryByUsernameAndPassword(String username, String password) {
        // 先根据用户名查询
        User user = new User();
        user.setUsername(username);
        user = userMapper.selectOne(user);
        // 判断是否为空
        if (user == null) {
            throw new LyException(ExceptionEnum.INVALID_UN_PW_ERROR);
        }
        // 对密码加密
        String pw = CodecUtils.md5Hex(password, user.getSalt());
        // 判断密码是否正确
        if(!user.getPassword().equals(pw)){
            throw new LyException(ExceptionEnum.INVALID_UN_PW_ERROR);
        }
        return user;
    }
}
