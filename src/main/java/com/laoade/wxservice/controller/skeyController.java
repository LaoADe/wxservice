package com.laoade.wxservice.controller;


import com.laoade.wxservice.domain.Author;
import com.laoade.wxservice.repository.AuthorRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class skeyController {
    private AuthorRepository authorRepository;
    private StringRedisTemplate redisTemplate;

    @Autowired
    public skeyController(AuthorRepository authorRepository, StringRedisTemplate redisTemplate) {
        this.authorRepository = authorRepository;
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @RequestMapping(value = "/skey", method = RequestMethod.POST)
    @ResponseBody
    public Map authorSkye(String skey){
        Map map=new HashMap();
        if(StringUtils.isNotBlank(skey)){
            // skey未失效，返回数据，更新缓存时间
            Author author = authorRepository.findBySkey(skey);
            String item = author.getItem();
            redisTemplate.expire(skey,240,TimeUnit.HOURS);
            map.put("success",1);
            map.put("msg","后台skey未失效");
            map.put("item",item);
            return map;
        }else {
            //失效，客户端重新发起登录请求
            map.put("msg","后台skey失效");
           return map;
        }
    }
}
