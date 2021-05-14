/**
 * 
 */
package cn.yunovo.job.executor.util.http;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.alibaba.fastjson.JSONObject;

/**
 * @author kk
 *
 * 2017年7月22日
 */


public class HttpEntityBuilder {
	public static HttpEntity build(String base64_auth, JSONObject jobj) {
		HttpHeaders headers = new HttpHeaders();
		MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
		headers.setContentType(type);
		headers.add("Accept", MediaType.APPLICATION_JSON.toString());
		headers.add("Authorization", "Basic "+base64_auth);
		return new org.springframework.http.HttpEntity<String>(jobj.toString(), headers);
	}
}
