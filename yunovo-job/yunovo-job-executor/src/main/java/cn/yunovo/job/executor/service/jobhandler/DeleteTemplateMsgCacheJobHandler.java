package cn.yunovo.job.executor.service.jobhandler;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;
import cn.yunovo.job.executor.util.redis.JedisPoolUtil;

@JobHandler(value="delete_template_msg_cache_lbt")
@Component

/*
 * 删除模板消息缓存
 * 
 * 
 */
public class DeleteTemplateMsgCacheJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(DeleteTemplateMsgCacheJobHandler.class);
    
    @Autowired
    public RestTemplate restTemplate;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private JedisPoolUtil jedisPoolUtil;
    
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
		try {
			deleteTemplateMsgCache();
			a.setCode(200);
			a.setMsg("success");
			logger.error("[DeleteTemplateMsgCacheJobHandler.deleteTemplateMsgCache][删除模板消息缓存成功]");
            XxlJobLogger.log("[DeleteTemplateMsgCacheJobHandler.deleteTemplateMsgCache][删除模板消息缓存成功]");
		} catch (Exception e) {
			e.printStackTrace();
            a.setCode(500);
            logger.error("[DeleteTemplateMsgCacheJobHandler.deleteTemplateMsgCache][删除模板消息缓存异常]");
            XxlJobLogger.log("[DeleteTemplateMsgCacheJobHandler.deleteTemplateMsgCache][删除模板消息缓存异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
		
		return a;
    }

	//删除缓存
	public void deleteTemplateMsgCache() throws Exception {
		String cacheKey = "Wechat#Configuration#Wx_appid#wxfcd75e38666c34eb";
		//String cacheKey = "Wechat#Configuration#Wx_appid#wxf120e49da368b3fd";
		//String cacheKey = "Wechat#Configuration#Wx_appid#common";
		String cacheData = jedisPoolUtil.get(cacheKey);
        XxlJobLogger.log("[DeleteTemplateMsgCacheJobHandler.deleteTemplateMsgCache]cacheData={0}",cacheData);
		if(!StringUtils.isEmpty(cacheData)) {
			jedisPoolUtil.del(cacheKey);
		}
		
	}
	
	//查询缓存
	public void queryCache() throws Exception {
		String cacheKey = "otaConf:OG-000172";
		List<String> list = jedisPoolUtil.hmget(cacheKey, "CK02_V1.0.0_TYZL-T23_Debug_1575011531");
		XxlJobLogger.log("[DeleteTemplateMsgCacheJobHandler.queryCache]result={0}",list);
	}
}
