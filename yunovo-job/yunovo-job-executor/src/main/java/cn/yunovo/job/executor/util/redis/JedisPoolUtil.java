package cn.yunovo.job.executor.util.redis;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisDataException;  
  
/** 
 * jedis的帮助工具 本类map依赖fastjson,对象与集合使用protostuff序列化框架 jedis版本2.9.0 
 * protostuff相关版本1.0.8 fastjson版本1.2.32 使用maven自动添加依赖 
 *  
 * @author Bill 
 * 
 */ 
@Component
public class JedisPoolUtil { 
	
    private static final int DEFAULT_SETEX_TIMEOUT = 60 * 60;// setex的默认时间  
  
    @Autowired
    private JedisPool jedisPool;
    
    public Jedis getJedis() {
    	
    	return jedisPool.getResource();
    }
    
    /** 
     * 添加一个字符串值,成功返回1,失败返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    public int set(String key, String value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            if (jedis.set(key, value).equalsIgnoreCase("ok")) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
  
    }  
  
    /** 
     * 缓存一个字符串值,成功返回1,失败返回0,默认缓存时间为1小时,以本类的常量DEFAULT_SETEX_TIMEOUT为准 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    public  int setEx(String key, String value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            if (jedis.setex(key, DEFAULT_SETEX_TIMEOUT, value).equalsIgnoreCase("ok")) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 缓存一个字符串值,成功返回1,失败返回0,缓存时间以timeout为准,单位秒 
     *  
     * @param key 
     * @param value 
     * @param timeout 
     * @return 
     */  
    public  int setEx(String key, String value, int timeout) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            if (jedis.setex(key, timeout, value).equalsIgnoreCase("ok")) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 添加一个指定类型的对象,成功返回1,失败返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    public  <T> int set(String key, T value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            byte[] data = enSeri(value);  
            if (jedis.set(key.getBytes(), data).equalsIgnoreCase("ok")) {
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 缓存一个指定类型的对象,成功返回1,失败返回0,默认缓存时间为1小时,以本类的常量DEFAULT_SETEX_TIMEOUT为准 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    public  <T> int setEx(String key, T value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            byte[] data = enSeri(value);  
            if (jedis.setex(key.getBytes(), DEFAULT_SETEX_TIMEOUT, data).equalsIgnoreCase("ok")) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 缓存一个指定类型的对象,成功返回1,失败返回0,缓存时间以timeout为准,单位秒 
     *  
     * @param key 
     * @param value 
     * @param timeout 
     * @return 
     */  
    public  <T> int setEx(String key, T value, int timeout) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            byte[] data = enSeri(value);  
            if (jedis.setex(key.getBytes(), timeout, data).equalsIgnoreCase("ok")) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 将一个数值+1,成功返回+后的结果,失败返回null 
     *  
     * @param key 
     * @return 
     * @throws JedisDataException 
     */  
    public  Long incr(String key) throws JedisDataException {  
        if (isValueNull(key)) {  
            return null;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            return jedis.incr(key);  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 将一个数值-1,成功返回-后的结果,失败返回null 
     *  
     * @param key 
     * @return 
     * @throws JedisDataException 
     */  
    public  Long decr(String key) throws JedisDataException {  
        if (isValueNull(key)) {  
            return null;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            return jedis.decr(key);  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 添加一个字符串值到list中,,成功返回1,失败返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    public  int setList(String key, String... value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            Long result = jedis.rpush(key, value);  
            if (result != null && result != 0) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 缓存一个字符串值到list中,全部list的key默认缓存时间为1小时,成功返回1,失败返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    public  int setExList(String key, String... value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            Long result = jedis.rpush(key, value);  
            jedis.expire(key, DEFAULT_SETEX_TIMEOUT);  
            if (result != null && result != 0) {  
                return 1;  
            } else {  
                return 0;  
            }  
  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 缓存一个字符串值到list中,全部list的key缓存时间为timeOut,单位为秒,成功返回1,失败返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    public  int setExList(String key, int timeOut, String... value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            Long result = jedis.rpush(key, value);  
            jedis.expire(key, timeOut);  
            if (result != null && result != 0) {  
                return 1;  
            } else {  
                return 0;  
            }  
  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 添加一个<T>类型对象值到list中,成功返回1,失败返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    @SafeVarargs  
    public final <T> int setList(String key, T... value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            int res = 0;  
            for (T t : value) {  
                byte[] data = enSeri(t);  
                Long result = jedis.rpush(key.getBytes(), data);  
                if (result != null && result != 0) {  
                    res++;  
                }  
            }  
            if (res != 0) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 缓存一个<T>类型对象值到list中,全部list的key默认缓存时间为1小时,成功返回1,失败返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    @SafeVarargs
    public  final <T> int setExList(String key, T... value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            int res = 0;  
            for (T t : value) {  
                byte[] data = enSeri(t);  
                Long result = jedis.rpush(key.getBytes(), data);  
                if (result != null && result != 0) {  
                    res++;  
                }  
            }  
            jedis.expire(key, DEFAULT_SETEX_TIMEOUT);  
            if (res != 0) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 缓存一个<T>类型对象值到list中,全部list的key缓存时间为timeOut,单位秒,成功返回1,失败返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    @SafeVarargs  
    public final <T> int setExList(String key, int timeOut, T... value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            int res = 0;  
            for (T t : value) {  
                byte[] data = enSeri(t);  
                Long result = jedis.rpush(key.getBytes(), data);  
                if (result != null && result != 0) {  
                    res++;  
                }  
            }  
            jedis.expire(key, timeOut);  
            if (res != 0) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 添加一个List集合成功返回1,失败返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     * @throws IOException 
     * @throws RuntimeException 
     */  
    public  <T> int setList(String key, List<T> value) throws RuntimeException, IOException {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            byte[] data = enSeriList(value);  
            if (jedis.set(key.getBytes(), data).equalsIgnoreCase("ok")) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 缓存一个List<T>集合,成功返回1,失败返回0,默认缓存时间为1小时,以本类的常量DEFAULT_SETEX_TIMEOUT为准 
     *  
     * @param key 
     * @param value 
     * @return 
     * @throws IOException 
     * @throws RuntimeException 
     */  
  
    public  <T> int setExList(String key, List<T> value) throws RuntimeException, IOException {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            byte[] data = enSeriList(value);  
            if (jedis.setex(key.getBytes(), DEFAULT_SETEX_TIMEOUT, data).equalsIgnoreCase("ok")) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 缓存一个List<T>集合,成功返回1,失败返回0,缓存时间以timeout为准,单位秒 
     *  
     * @param key 
     * @param value 
     * @param timeout 
     * @return 
     * @throws IOException 
     * @throws RuntimeException 
     */  
    public  <T> int setExList(String key, List<T> value, int timeout) throws RuntimeException, IOException {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            byte[] data = enSeriList(value);  
            if (jedis.setex(key.getBytes(), timeout, data).equalsIgnoreCase("ok")) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 添加一个字符串到set,如果key存在就在就最追加,如果key不存在就创建,成功返回1,失败或者没有受影响返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    public  int setSet(String key, String... value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            Long result = jedis.sadd(key, value);  
            if (result != null && result != 0) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 添加一个字符串set,如果key存在就在就最追加,整个set的key默认一小时后过期,如果key存在就在可以种继续添加,如果key不存在就创建,成功返回1,失败或者没有受影响返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    public  int setExSet(String key, String... value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            Long result = jedis.sadd(key, value);  
            jedis.expire(key, DEFAULT_SETEX_TIMEOUT);  
            if (result != null && result != 0) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 添加一个字符串set,如果key存在就在就最追加,整个set的key有效时间为timeOut时间,单位秒,如果key存在就在可以种继续添加,如果key不存在就创建,,成功返回1,失败或者没有受影响返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    public  int setExSet(String key, int timeOut, String... value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            Long result = jedis.sadd(key, value);  
            jedis.expire(key, timeOut);  
            if (result != null && result != 0) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 添加一个<T>类型到set集合,如果key存在就在就最追加,成功返回1,失败或者没有受影响返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    @SafeVarargs  
    public final <T> int setSet(String key, T... value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            int res = 0;  
            for (T t : value) {  
                byte[] data = enSeri(t);  
                Long result = jedis.sadd(key.getBytes(), data);  
                if (result != null && result != 0) {  
                    res++;  
                }  
            }  
            if (res != 0) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 缓存一个<T>类型到set集合,如果key存在就在就最追加,整个set的key默认有效时间为1小时,成功返回1,失败或者没有受影响返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    @SafeVarargs  
    public final <T> int setExSet(String key, T... value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            int res = 0;  
            for (T t : value) {  
                byte[] data = enSeri(t);  
                Long result = jedis.sadd(key.getBytes(), data);  
                if (result != null && result != 0) {  
                    res++;  
                }  
            }  
            jedis.expire(key, DEFAULT_SETEX_TIMEOUT);  
            if (res != 0) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 缓存一个<T>类型到set集合,如果key存在就在就最追加,整个set的key有效时间为timeOut,单位秒,成功返回1,失败或者没有受影响返回0 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    @SafeVarargs  
    public  final <T> int setExSet(String key, int timeOut, T... value) {  
        if (isValueNull(key, value)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            int res = 0;  
            for (T t : value) {  
                byte[] data = enSeri(t);  
                Long result = jedis.sadd(key.getBytes(), data);  
                if (result != null && result != 0) {  
                    res++;  
                }  
            }  
            jedis.expire(key, timeOut);  
            if (res != 0) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 添加一个Map<K, V>集合,成功返回1,失败返回0 
     *  
     * @param key 
     * @param value 
     * @param timeout 
     * @return 
     */  
    public  <K, V> int setMap(String key, Map<K, V> value) {  
        if (value == null || key == null || "".equals(key)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            String data = JSON.toJSONString(value);  
            if (jedis.set(key, data).equalsIgnoreCase("ok")) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 缓存一个Map<K, V>集合,成功返回1,失败返回0,默认缓存时间为1小时,以本类的常量DEFAULT_SETEX_TIMEOUT为准 
     *  
     * @param key 
     * @param value 
     * @param timeout 
     * @return 
     */  
    public  <K, V> int setExMap(String key, Map<K, V> value) {  
        if (value == null || key == null || "".equals(key)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            String data = JSON.toJSONString(value);  
            if (jedis.setex(key, DEFAULT_SETEX_TIMEOUT, data).equalsIgnoreCase("ok")) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }
    
    /**
     * 
     * @param key
     * @param value
     * @param timeOut
     * @return
     */
    public <K, V> int hmset(String key, Map<String, String> value, int timeOut) {
    	
        Jedis jedis = null;
        try {  
            jedis = this.getJedis(); 
            String res = jedis.hmset(key, value);
            jedis.expire(key, timeOut);  
            if (StringUtils.equalsIgnoreCase(res, "ok")) {  
                return 1;  
            } else {
                return 0;  
            }
        } finally {  
            this.closeJedis(jedis);  
        } 
    }
    
    /**
     * 缓存hase对象
     * @param key
     * @param value
     * @param timeOut
     * @return
     */
    public <K, V> int hmset(String key, Map<String, String> value) {
    	
        Jedis jedis = null;
        try {  
            jedis = this.getJedis(); 
            String res = jedis.hmset(key, value);
            if (StringUtils.equalsIgnoreCase(res, "ok")) {  
                return 1;  
            } else {
                return 0;  
            }
        } finally {  
            this.closeJedis(jedis);  
        } 
    }
    
    public List<String> hmget(String key, String... fields) {
    	 if (key == null || "".equals(key)) {  
             return null;  
         }
         Jedis jedis = null;
         try {  
             jedis = this.getJedis();
             List<String> data = jedis.hmget(key, fields);
             return data;  
         } finally {  
             this.closeJedis(jedis);  
         }
    }
    
	public Set<String> hkeys(String key) {
		
		if (StringUtils.isEmpty(key)) {  
            return null;  
        }
		Jedis jedis = null;
		try {
			jedis = this.getJedis();
			return jedis.hkeys(key);
		} finally {
			this.closeJedis(jedis);
		}
	}
	
	public void hdel(String key, String... fields) {
		
		Jedis jedis = null;
		try {
			jedis = this.getJedis();
			jedis.hdel(key, fields);
		} finally {
			this.closeJedis(jedis);
		}
	}
  
    /** 
     * 缓存一个Map<K, V>集合,成功返回1,失败返回0,缓存时间以timeout为准,单位秒 
     *  
     * @param key 
     * @param value 
     * @param timeout 
     * @return 
     */  
    public  <K, V> int setExMap(String key, Map<K, V> value, int timeout) {  
        if (value == null || key == null || "".equals(key)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            String data = JSON.toJSONString(value);  
            if (jedis.setex(key, timeout, data).equalsIgnoreCase("ok")) {  
                return 1;  
            } else {  
                return 0;  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 获取一个字符串值 
     *  
     * @param key 
     * @return 
     */  
    public  String get(String key) {  
        if (isValueNull(key)) {  
            return null;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            return jedis.get(key);  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 获得一个指定类型的对象 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    public  <T> T get(String key, Class<T> clazz) {  
        if (isValueNull(key)) {  
            return null;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
  
            byte[] data = jedis.get(key.getBytes());  
            T result = deSeri(data, clazz);  
            return result;  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 获得一个字符串集合,区间以偏移量 START 和 END 指定。 其中 0 表示列表的第一个元素， 1 
     * 表示列表的第二个元素，以此类推。 你也可以使用负数下标，以 -1 表示列表的最后一个元素， -2 表示列表的倒数第二个元素，以此类推。 
     *  
     * @param key 
     * @param start 
     * @param end 
     * @return 
     */  
    public  List<String> getList(String key, long start, long end) {  
        if (isValueNull(key)) {  
            return null;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            List<String> result = jedis.lrange(key, start, end);  
            return result;  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 获得一个<T>类型的对象集合,区间以偏移量 START 和 END 指定。 其中 0 表示列表的第一个元素， 1 表示列表的第二个元素，以此类推。 
     * 你也可以使用负数下标，以 -1 表示列表的最后一个元素， -2 表示列表的倒数第二个元素，以此类推。 
     *  
     * @param key 
     * @param start 
     * @param end 
     * @return 
     */  
    public  <T> List<T> getList(String key, long start, long end, Class<T> clazz) {  
        if (isValueNull(key)) {  
            return null;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            List<byte[]> lrange = jedis.lrange(key.getBytes(), start, end);  
            List<T> result = null;  
            if (lrange != null) {  
                for (byte[] data : lrange) {  
                    if (result == null) {  
                        result = new ArrayList<>();  
                    }  
                    result.add(deSeri(data, clazz));  
                }  
            }  
            return result;  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 获得list中存了多少个值 
     *  
     * @return 
     */  
    public  long getListCount(String key) {  
        if (isValueNull(key)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            return jedis.llen(key);  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 获得一个List<T>的集合, 
     *  
     * @param key 
     *            键 
     * @param clazz 
     *            返回集合的类型 
     * @return 
     * @throws IOException 
     */  
    public  <T> List<T> getList(String key, Class<T> clazz) throws IOException {  
        if (isValueNull(key)) {  
            return null;  
        }  
        Jedis jedis = null;  
        try {
            jedis = this.getJedis();  
            byte[] data = jedis.get(key.getBytes());  
            List<T> result = deSeriList(data, clazz);  
            return result;  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 获得一个字符串set集合 
     *  
     * @param key 
     * @return 
     */  
    public  Set<String> getSet(String key) {  
        if (isValueNull(key)) {  
            return null;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            Set<String> result = jedis.smembers(key);  
            return result;  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 获得一个字符串set集合 
     *  
     * @param key 
     * @return 
     */  
    public  <T> Set<T> getSet(String key, Class<T> clazz) {  
        if (isValueNull(key)) {  
            return null;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            Set<byte[]> smembers = jedis.smembers(key.getBytes());  
            Set<T> result = null;  
            if (smembers != null) {  
                for (byte[] data : smembers) {  
                    if (result == null) {  
                        result = new HashSet<>();  
                    }  
                    result.add(deSeri(data, clazz));  
                }  
            }  
            return result;  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 获得集合中存在多少个值 
     *  
     * @param key 
     * @return 
     */  
    public  long getSetCount(String key) {  
        if (isValueNull(key)) {  
            return 0;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            return jedis.scard(key);  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 获得一个Map<v,k>的集合 
     *  
     * @param key 
     * @param v 
     * @param k 
     * @return 
     */  
    public  <K, V> Map<K, V> getMap(String key, Class<K> k, Class<V> v) {  
        if (key == null || "".equals(key)) {  
            return null;  
        }  
        Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            String data = jedis.get(key);  
            @SuppressWarnings("unchecked")  
            Map<K, V> result = (Map<K, V>) JSON.parseObject(data);  
            return result;  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    /** 
     * 删除一个值 
     *  
     * @param key 
     */  
    public  void del(String... key) {  
    	Jedis jedis = null;  
        try {  
            jedis = this.getJedis();  
            for (int i = 0; i < key.length; i++) {  
                jedis.del(key[i]);  
            }  
        } finally {  
            this.closeJedis(jedis);  
        }  
    }  
  
    // --------------------------公用方法区------------------------------------  
    /** 
     * 检查值是否为null,如果为null返回true,不为null返回false 
     *  
     * @param obj 
     * @return 
     */  
    private  boolean isValueNull(Object... obj) {  
        for (int i = 0; i < obj.length; i++) {  
            if (obj[i] == null || "".equals(obj[i])) {  
                return true;  
            }  
        }  
        return false;  
    }  
  
    /** 
     * 序列化一个对象 
     *  
     * @param value 
     * @return 
     */  
    private  <T> byte[] enSeri(T value) {  
        @SuppressWarnings("unchecked")  
        RuntimeSchema<T> schema = (RuntimeSchema<T>) RuntimeSchema.createFrom(value.getClass());  
        byte[] data = ProtostuffIOUtil.toByteArray(value, schema,  
                LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));  
        return data;  
    }  
  
    /** 
     * 反序列化一个对象 
     *  
     * @param t 
     * @return 
     */  
    private  <T> T deSeri(byte[] data, Class<T> clazz) {  
        if (data == null || data.length == 0) {  
            return null;  
        }  
        RuntimeSchema<T> schema = RuntimeSchema.createFrom(clazz);  
        T result = schema.newMessage();  
        ProtostuffIOUtil.mergeFrom(data, result, schema);  
        return result;  
    }  
  
    /** 
     * 序列化List集合 
     *  
     * @param list 
     * @return 
     * @throws IOException 
     */  
    private  <T> byte[] enSeriList(List<T> list) throws RuntimeException, IOException {  
        if (list == null || list.size() == 0) {  
            throw new RuntimeException("集合不能为空!");  
        }  
        @SuppressWarnings("unchecked")  
        RuntimeSchema<T> schema = (RuntimeSchema<T>) RuntimeSchema.getSchema(list.get(0).getClass());  
        ByteArrayOutputStream out = new ByteArrayOutputStream();  
        ProtostuffIOUtil.writeListTo(out, list, schema, LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));  
        byte[] byteArray = out.toByteArray();  
        return byteArray;  
    }  
  
    /** 
     * 反序列化List集合 
     *  
     * @param data 
     * @param clazz 
     * @return 
     * @throws IOException 
     */  
    private  <T> List<T> deSeriList(byte[] data, Class<T> clazz) throws IOException {  
        if (data == null || data.length == 0) {  
            return null;  
        }  
        RuntimeSchema<T> schema = RuntimeSchema.createFrom(clazz);  
        List<T> result = ProtostuffIOUtil.parseListFrom(new ByteArrayInputStream(data), schema);  
        return result;  
    }  
  
    public void closeJedis(Jedis jedis) {  
        if (jedis != null) {
        	jedis.close();
        }  
    } 
    
}  