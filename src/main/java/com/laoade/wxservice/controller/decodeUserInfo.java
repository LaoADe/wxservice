package com.laoade.wxservice.controller;

/**
 * @Description: 解密用户敏感数据
 * @param encryptedData 明文,加密数据
 * @param iv   加密算法的初始向量
 * @param code  用户允许登录后，回调内容会带上 code（有效期五分钟），开发者需要将 code 发送到开发者服务器后台，使用code 换取 session_key api，将 code 换成 openid 和 session_key
 * @return 返回数据不应携带敏感信息openId,session_key
 */


import com.alibaba.fastjson.JSONObject;
import com.laoade.wxservice.domain.Author;
import com.laoade.wxservice.repository.AuthorRepository;
import com.laoade.wxservice.util.AesCbcUtil;
import com.laoade.wxservice.util.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class decodeUserInfo {
    private final AuthorRepository authorRepository;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public decodeUserInfo(AuthorRepository authorRepository, StringRedisTemplate redisTemplate) {
        this.authorRepository = authorRepository;
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @RequestMapping(value = "/decodeUserInfo", method = RequestMethod.POST)
    @ResponseBody

    public Map decodeUserInfo(String encryptedData, String iv, String code) {
        Map map = new HashMap();
        String skey;
        // 登录凭证不能为空
        if (code == null || code.length() == 0) {
            map.put("status", 0);
            map.put("msg", "code 不能为空");
            return map;
        }

        // 小程序唯一标识 (在微信小程序管理后台获取)
        //TODO：小程序唯一标识
        String wxspAppid = "";
        // 小程序的 app secret (在微信小程序管理后台获取)
        //TODO：wxspSecret
        String wxspSecret = "";
        // 授权（必填）
        String grant_type = "authorization_code";

        //////////////// 1、向微信服务器 使用登录凭证 code 获取 session_key 和 openid
        //////////////// ////////////////
        // 请求参数
        String params = "appid=" + wxspAppid + "&secret=" + wxspSecret + "&js_code=" + code + "&grant_type="
                + grant_type;
        // 发送请求
        String sr = HttpRequest.sendGet("https://api.weixin.qq.com/sns/jscode2session", params);
        // 解析相应内容（转换成json对象）
        JSONObject json = JSONObject.parseObject(sr);
        // 获取会话密钥（session_key）
        String session_key = json.get("session_key").toString();
        // 用户的唯一标识（openid）
        String openid = (String) json.get("openid");

        //////////////// 2、对encryptedData加密数据进行AES解密 ////////////////
        try {
            String result = AesCbcUtil.decrypt(encryptedData, session_key, iv, "UTF-8");
            if (null != result && result.length() > 0) {
                map.put("status", 1);
                map.put("msg", "解密成功");
                JSONObject userInfoJSON = JSONObject.parseObject(result);
                // 获取用户数据
                String nickName = (String) userInfoJSON.get("nickName");
                Integer gender = (Integer) userInfoJSON.get("gender");
                String city = (String) userInfoJSON.get("city");
                String province = (String) userInfoJSON.get("province");
                String country = (String) userInfoJSON.get("country");
                String avatarUrl = (String) userInfoJSON.get("avatarUrl");
                // 与公众号可以获取unionId
                String unionId = openid + 1;
                Map userInfo = new HashMap();
                userInfo.put("nickName", nickName);
                userInfo.put("gender", gender);
                userInfo.put("city", city);
                userInfo.put("province", province);
                userInfo.put("country", country);
                // 自定义登录态skey
                skey = UUID.randomUUID().toString();
                JSONObject sessionObj = new JSONObject();
                sessionObj.put("openId", openid);
                sessionObj.put("sessionKey", session_key);
                // 以skey为键，openId和sessionKey为值缓存
                redisTemplate.opsForValue().set(skey, sessionObj.toJSONString(), 240, TimeUnit.HOURS);
                userInfo.put("skey", skey);
                Author author = authorRepository.findByOpenId(openid);
                if (author == null) {
                    // 如果用户不存在，添加
                    author = new Author();
                }
                // 存在更新
                authorAdd(author, openid, nickName, gender, city, province, country, avatarUrl, unionId, skey);
                map.put("userInfo", userInfo);
            } else {
                map.put("status", 0);
                map.put("msg", "解密失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(map);
        return map;
    }
    //存库or更新
    private void authorAdd(Author author,
                           String openId,
                           String nickName,
                           Integer gender,
                           String city,
                           String province,
                           String country,
                           String avatarUrl,
                           String unionId,
                           String skey) {
        author.setNickName(nickName);
        author.setOpenId(openId);
        author.setGender(gender);
        author.setCity(city);
        author.setProvince(province);
        author.setCountry(country);
        author.setAvatarUrl(avatarUrl);
        author.setUnionId(unionId);
        author.setItem(null);
        author.setSkey(skey);
        authorRepository.save(author);
    }
}