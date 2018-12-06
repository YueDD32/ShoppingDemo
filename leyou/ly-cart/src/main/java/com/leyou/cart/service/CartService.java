package com.leyou.cart.service;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.cart.interceptors.UserInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:add:uid:";

    public void addCart(Cart cart) {
        // 获取用户
        UserInfo user = UserInterceptor.getUser();
        // 获取用户id, 拼接key
        String key = KEY_PREFIX + user.getId();
        // 获取操作对象
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(key);

        // 判断是否存在
        String hKey = cart.getSkuId().toString();
        if (hashOps.hasKey(hKey)) {
            // 记录新添加的数量
            Integer num = cart.getNum();
            // 存在，则修改数量
            String json = hashOps.get(hKey);
            cart = JsonUtils.toBean(json, Cart.class);
            cart.setNum(cart.getNum() + num);
        }
        // 写回redis
        hashOps.put(hKey, JsonUtils.toString(cart));
    }

    public List<Cart> queryCartList() {
        // 获取用户
        UserInfo user = UserInterceptor.getUser();
        // 获取用户id, 拼接key
        String key = KEY_PREFIX + user.getId();
        // 判断
        if(!redisTemplate.hasKey(key)){
            throw new LyException(ExceptionEnum.INSERT_BRAND_ERROR);
        }
        // 获取操作对象
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(key);
        // 获取用户购物车
        List<Cart> list = hashOps.values().stream()
                .map(s -> JsonUtils.toBean(s, Cart.class))
                .collect(Collectors.toList());
        return list;
    }
}
