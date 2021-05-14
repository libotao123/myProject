package cn.yunovo.job.admin.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.admin.common.HttpKit;
import cn.yunovo.job.core.biz.model.ReturnT;

/**
 * @author zzy 2018-10-18 14:13:16
 */
@Controller
@RequestMapping("/secure/rabbitMq")
public class RabbitMqController {
	
	private static Logger logger = LoggerFactory.getLogger(RabbitMqController.class);
	
	@Value("${rabbitMq.path}")
    private String path;
	
	@Value("${rabbitMq.username}")
    private String username;
	
	@Value("${rabbitMq.password}")
    private String password;
	
	@RequestMapping("/link")
	public String linkRabbitMq() {
		return "rabbitMq";
	}
	
	@RequestMapping("/getConnections")
	@ResponseBody
	public ReturnT<JSONArray> getConnections() {
		JSONArray resultJson = new JSONArray();
        try {
        	String str = HttpKit.Get(path + "/api/connections", username, password);
        	JSONArray strToJson = JSONArray.parseArray(str);
            for (Object jb : strToJson) {
            	JSONObject newJson = new JSONObject();
            	JSONObject jb_ = JSONObject.parseObject(jb.toString());
            	newJson.put("Vhost", jb_.get("vhost"));
            	newJson.put("name", jb_.get("name"));
            	newJson.put("user_provided_name", jb_.get("user_provided_name"));
            	newJson.put("user", jb_.get("user"));
            	newJson.put("state", jb_.get("state"));
            	newJson.put("protocol", jb_.get("protocol"));
            	newJson.put("channels", jb_.get("channels"));
            	resultJson.add(newJson);
            }
        } catch (IOException e) {
            logger.error("请求 rabbitMq Api 报错",e);
            return new ReturnT<JSONArray>(500,"请求 rabbitMq Api 报错");
        }

		return new ReturnT<JSONArray>(resultJson);
	}
	
	@RequestMapping("/getChannels")
	@ResponseBody
	public ReturnT<String> getChannels() {
		String str = "";
		try {
        	str = HttpKit.Get(path + "/api/channels", username, password);
        } catch (IOException e) {
            logger.error("请求 rabbitMq Api 报错",e);
            return new ReturnT<String>(500,"请求 rabbitMq Api 报错");
        }
        return new ReturnT<String>(str);
	}
	
	@RequestMapping("/getQueues")
	@ResponseBody
	public ReturnT<String> getQueues() {
        String result = null;
        try {
            result = HttpKit.Get(path + "/api/queues", username, password);
        } catch (IOException e) {
        	logger.error("请求 rabbitMq Api 报错",e);
            return new ReturnT<String>(500,"请求 rabbitMq Api 报错");
        }
        return new ReturnT<String>(result);
	}

	
	
}
