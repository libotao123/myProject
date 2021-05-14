//package cn.yunovo.job.executor.util;
//
//
//import java.util.Map;
//import java.util.Set;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.web.client.RestTemplate;
//
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//
//@Configuration
//public class YunDashQueryUtil {
//	
//	protected final static Logger logger = LoggerFactory.getLogger(YunDashQueryUtil.class);
//	
//	@Value("${yundash.url}")
//	private String yundash_url;
//	
//	@Value("${yundash.api_key}")
//	private String yundash_api_key;
//	
//	@Autowired
//	public RestTemplate restTemplate;
//	
//	/**
//	 * 主查询
//	 * @return
//	 */
//	public JSONArray queryMainYundash (Map<String, Object> param, String query_id) throws Exception {
//		String param_url = "";
//		Set<String> set = param.keySet();
//		for (String key:set) {
//			param_url += "&p_" + key + "=" + param.get(key);
//		}
//		JSONObject refresh_result = null;
//		String result_url = String.format(yundash_url+"/api/queries/%s/refresh?api_key=%s", query_id,yundash_api_key) + param_url;
//		refresh_result = refreshYundash(result_url);
//		String id = refresh_result.getJSONObject("job").getString("id");
//		Integer query_result_id = getYundashJobId(yundash_url+"/api/jobs/%s?api_key=%s",id,1);
//		if (query_result_id == -1) {
//			logger.error("yundash查询超时");
//			return null;
//		}
//		JSONArray doc_count_arr = getYundashQueryResultId(yundash_url+"/api/query_results/%s?api_key=%s",query_result_id);
//		return doc_count_arr;
//	}
//	
//	/**
//	 * 刷新yundash
//	 */
//	public JSONObject refreshYundash(String url){
//		HttpHeaders headers = new HttpHeaders();
//		MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
//		headers.setContentType(type);
//		headers.add("Accept", MediaType.APPLICATION_JSON.toString());
//		String result = restTemplate.postForObject(url, headers, String.class);
//		return JSONObject.parseObject(result);
//	}
//	
//	/**
//	 * 获取yundash的jobid 
//	 */
//	public int getYundashJobId(String url, String id, int index) throws InterruptedException{
//		String result = restTemplate.getForObject(String.format(url, id,yundash_api_key), String.class);
//		JSONObject job_result = JSONObject.parseObject(result);
//		if (null != job_result.getJSONObject("job").getInteger("query_result_id")) {
//			return job_result.getJSONObject("job").getInteger("query_result_id");
//		} else {
//			if (index>200) {
//				return -1;
//			}
//			Thread.sleep(300);
//			index++;
//			return getYundashJobId(url, id, index);
//		}
//	}
//	
//	/**
//	 * 根据查询id获取yundash数据 
//	 */
//	public JSONArray getYundashQueryResultId(String url,Integer query_result_id) {
//		String result = restTemplate.getForObject(String.format(url, query_result_id,yundash_api_key), String.class);
//		JSONObject res = JSONObject.parseObject(result);
//		return res.getJSONObject("query_result").getJSONObject("data").getJSONArray("rows");
//	}
//	
//	/**
//	 * 不带参数查询
//	 */
//	public JSONArray queryMainYundash(String url) throws Exception {
//		String result = restTemplate.getForObject(url, String.class);
//		JSONObject res = JSONObject.parseObject(result);
//		return res.getJSONObject("query_result").getJSONObject("data").getJSONArray("rows");
//	}
//
//}
