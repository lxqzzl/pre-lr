package com.test.ge.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.binary.Base64;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Jwt工具类
 *
 * @author lxq
 */

public class JwtUtil {
	
    //创建默认的秘钥和算法，供无参的构造方法使用
    private static final String defaultbase64EncodedSecretKey = "key";
    private static final SignatureAlgorithm defaultsignatureAlgorithm = SignatureAlgorithm.HS256;

    /**
     * 无参的构造方法
     */
    public JwtUtil() {
        this(defaultbase64EncodedSecretKey, defaultsignatureAlgorithm);
    }

    
    private final String base64EncodedSecretKey;
    private final SignatureAlgorithm signatureAlgorithm;

    /**
     * 有参的构造方法
     * @param secretKey 秘钥 
     * @param signatureAlgorithm 算法
     */
    public JwtUtil(String secretKey, SignatureAlgorithm signatureAlgorithm) {
        this.base64EncodedSecretKey = Base64.encodeBase64String(secretKey.getBytes());
        this.signatureAlgorithm = signatureAlgorithm;
    }

    /*
     * 生成jwt字符串
     * jwt字符串包括三个部分
     *  1. header
     *      -当前字符串的类型，一般都是“JWT”
     *      -哪种算法加密，“HS256”或者其他的加密算法
     *      所以一般都是固定的，没有什么变化
     *  2. payload
     *      一般有四个最常见的标准字段（下面有）
     *      iat：签发时间，也就是这个jwt什么时候生成的
     *      jti：JWT的唯一标识
     *      iss：签发人，一般都是username或者userId
     *      exp：过期时间
     *
     * */
    public String sign(String iss, long ttlMillis, Map<String, Object> claims) {
        //iss签发人，ttlMillis生存时间，claims是指还想要在jwt中存储的一些非隐私信息
        if (claims == null) {
            claims = new HashMap<String, Object>();
        }
        long nowMillis = System.currentTimeMillis();

        JwtBuilder builder = Jwts.builder()
                .setClaims(claims)
                //1. 这个是JWT的唯一标识，一般设置成唯一的，这个方法可以生成唯一标识
                .setId(UUID.randomUUID().toString())
                //2. 这个地方是以毫秒为单位，换算当前系统时间生成的iat
                .setIssuedAt(new Date(nowMillis))
                //3. 签发人，也就是JWT是给谁的（逻辑上一般都是username或者userId）
                .setSubject(iss)
                //4. 这个地方是生成jwt使用的算法和秘钥
                .signWith(signatureAlgorithm, base64EncodedSecretKey);
        
        if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            //5. 过期时间，这个也是使用毫秒生成的，使用当前时间+前面传入的持续时间生成
            builder.setExpiration(exp);
        }
        return builder.compact();
    }

    /**
     * 解析jwt字符串，拿到荷载部分所有的键值对
     * @param jwtToken
     * @return Claims 键值对
     */
    public JSONObject decode(String jwtToken) {
    	JSONObject jsonObject= new JSONObject();
       	// 得到 DefaultJwtParser
    	Claims claims= Jwts.parser()
                // 设置签名的秘钥
                .setSigningKey(base64EncodedSecretKey)
                // 设置需要解析的 jwt
                .parseClaimsJws(jwtToken)
                .getBody();  
    	for(String key:claims.keySet()) {
    		jsonObject.put(key,claims.get(key));
    	}
    	return jsonObject;
    }

    /**
     * 解析jwt字符串，拿到荷载部分指定的键值对
     * @param jwtToken
     * @return Claims 键值对
     */
    public JSONObject decodeKey(String jwtToken, String key) {
    	JSONObject jsonObject= new JSONObject();
    	// 得到 DefaultJwtParser
    	Claims claims= Jwts.parser()
                // 设置签名的秘钥
                .setSigningKey(base64EncodedSecretKey)
                // 设置需要解析的 jwt
                .parseClaimsJws(jwtToken)
                .getBody();   
    	jsonObject.put(key, claims.get(key));
        return jsonObject;
    }
    
    
    /**
     * 判断jwtToken是否合法
     * @param jwtToken
     * @return boolean 
     */
    public Boolean isVerify(String jwtToken) {
        //这个是官方的校验规则，这里只写了一个”校验算法“，可以自己加
        Algorithm algorithm = null;
        switch (signatureAlgorithm) {
            case HS256:
                algorithm = Algorithm.HMAC256(Base64.decodeBase64(base64EncodedSecretKey));
                break;
            default:
                throw new RuntimeException("不支持该算法");
        }
        JWTVerifier verifier = JWT.require(algorithm).build();
        // 校验不通过会抛出异常
        try {
            verifier.verify(jwtToken);  
        } catch (Exception e) {
        	return false;
		}
        //判断合法的标准：1. 头部和荷载部分没有篡改过。2. 没有过期
        return true;
    }
}

