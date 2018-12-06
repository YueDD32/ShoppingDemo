package com.leyou.auth.utils;

import com.leyou.auth.constants.JwtConstants;
import com.leyou.auth.pojo.UserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.joda.time.DateTime;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author: HuYi.Zhang
 * @create: 2018-05-26 15:43
 **/
public class JwtUtils {
    /**
     * 私钥加密token
     *
     * @param userInfo      载荷中的数据
     * @param privateKey    私钥
     * @param expireMinutes 过期时间，单位分钟
     * @return
     * @throws Exception
     */
    public static String generateToken(UserInfo userInfo, PrivateKey privateKey, int expireMinutes) throws Exception {
        return Jwts.builder()
                .claim(JwtConstants.JWT_KEY_ID, userInfo.getId())
                .claim(JwtConstants.JWT_KEY_USER_NAME, userInfo.getUsername())
                .setExpiration(DateTime.now().plusMinutes(expireMinutes).toDate())
                .signWith(SignatureAlgorithm.RS256, privateKey)
                .compact();
    }

    /**
     * 私钥加密token
     *
     * @param userInfo      载荷中的数据
     * @param privateKey    私钥字节数组
     * @param expireMinutes 过期时间，单位秒
     * @return
     * @throws Exception
     */
    public static String generateToken(UserInfo userInfo, byte[] privateKey, int expireMinutes) throws Exception {
        return Jwts.builder()
                .claim(JwtConstants.JWT_KEY_ID, userInfo.getId())
                .claim(JwtConstants.JWT_KEY_USER_NAME, userInfo.getUsername())
                .setExpiration(DateTime.now().plusMinutes(expireMinutes).toDate())
                .signWith(SignatureAlgorithm.RS256, RsaUtils.getPrivateKey(privateKey))
                .compact();
    }

    /**
     * 公钥解析token
     *
     * @param token     用户请求中的token
     * @param publicKey 公钥
     * @return
     * @throws Exception
     */
    private static Jws<Claims> parserToken(String token, PublicKey publicKey) {
        return Jwts.parser().setSigningKey(publicKey).parseClaimsJws(token);
    }

    /**
     * 公钥解析token
     *
     * @param token     用户请求中的token
     * @param publicKey 公钥字节数组
     * @return
     * @throws Exception
     */
    private static Jws<Claims> parserToken(String token, byte[] publicKey) throws Exception {
        return Jwts.parser().setSigningKey(RsaUtils.getPublicKey(publicKey))
                .parseClaimsJws(token);
    }

    /**
     * 获取token中的用户信息
     *
     * @param token     用户请求中的令牌
     * @param publicKey 公钥
     * @return 用户信息
     * @throws Exception
     */
    public static UserInfo getInfoFromToken(String token, PublicKey publicKey) throws Exception {
        Jws<Claims> claimsJws = parserToken(token, publicKey);
        Claims body = claimsJws.getBody();
        return new UserInfo(
                ObjectUtils.toLong(body.get(JwtConstants.JWT_KEY_ID)),
                ObjectUtils.toString(body.get(JwtConstants.JWT_KEY_USER_NAME))
        );
    }

    /**
     * 获取token中的用户信息
     *
     * @param token     用户请求中的令牌
     * @param publicKey 公钥
     * @return 用户信息
     * @throws Exception
     */
    public static UserInfo getInfoFromToken(String token, byte[] publicKey) throws Exception {
        Jws<Claims> claimsJws = parserToken(token, publicKey);
        Claims body = claimsJws.getBody();
        return new UserInfo(
                ObjectUtils.toLong(body.get(JwtConstants.JWT_KEY_ID)),
                ObjectUtils.toString(body.get(JwtConstants.JWT_KEY_USER_NAME))
        );
    }

 /*   public static void main(String[] args) throws Exception {
        // 载荷
        UserInfo userInfo = new UserInfo(3L, "JackMa");
        // 私钥
        String privateKeyFilename = "C:\\lesson\\heima46\\ssh\\id_rsa.pri";
        PrivateKey privateKey = RsaUtils.getPrivateKey(privateKeyFilename);
        String publicKeyFilename = "C:\\lesson\\heima46\\ssh\\id_rsa.pub";
        PublicKey publicKey = RsaUtils.getPublicKey(publicKeyFilename);

        // 生成token
        String token = generateToken(userInfo, privateKey, 5);
//        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6MywidXNlcm5hbWUiOiJKYWNrTWEiLCJleHAiOjE1NDI4NzUyNjB9.iFh49iyjpMEg416_Yi4oItsNcJKDMVvvuRquD1tX_ScNAgG5xYEy3xNWA_w-8ri0mwtNbMBQKA1v45TO1yFXHIPoSqPYqZUpxHqiBWZJ_-fha6dhuYrjHypoM0f_Pyp1PoK4lCYhIlsOXLhZRxKVUGMZBTylcZehLgu8Un2PkrA";
        System.out.println("token = " + token);

        Thread.sleep(5000L);
        // 解析token
        UserInfo info = getInfoFromToken(token, publicKey);
        System.out.println("info = " + info);
    }*/
}