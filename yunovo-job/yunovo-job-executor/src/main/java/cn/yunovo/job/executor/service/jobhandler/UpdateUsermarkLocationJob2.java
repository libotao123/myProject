package cn.yunovo.job.executor.service.jobhandler;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;


@JobHandler(value="usermark_location_job2")
@Component
public class UpdateUsermarkLocationJob2 extends IJobHandler {

	// yundash配置
    private String yundash_url = "http://yundash.yunovo.cn";
    private String yundash_api_key = "EKUtEeFlJwBwbTM2O9fRglcoMycHLR1M2MjX5vZ9";
    private String traces_query_id1 = "1045";
    private String traces_query_id2 = "1050";
    private String driving_query_id1 = "1066";
    private String driving_query_id2 = "1070";
    
    private final long diff_time = 43200000;//12*3600*1000,12H
    
    private final long max_time = 7776000000L;//90*86400000,90天
    
	/**根据经纬度获取地理位置*/
	String getLocation_url = "http://192.168.3.240:7000/rest/api/lbs/getLocation?longitude=%s&latitude=%s&coordtype=%s";
//	String getLocation_url = "http://isapi.prd.yunovo.cn/rest/api/lbs/getLocation?longitude=%s&latitude=%s&coordtype=%s";
	
	String getLastPoint_url = "http://192.168.3.240:9020/rest/api/trace/getLastPointByDid?did=%s";
//	String getLastPoint_url = "http://lsapi.prd.yunovo.cn/rest/api/trace/getLastPointByDid?did=%s";
	
	@Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    public RestTemplate restTemplate;
	
	@Override
	public ReturnT<String> execute(String param) throws Exception {
		ReturnT<String> a = new ReturnT<String>();
		try {
			main();
			a.setCode(200);
			a.setMsg("success");
		} catch (Exception e) {
			e.printStackTrace();
            a.setCode(500);
            XxlJobLogger.log("[UpdateUsermarkLocationJob.main][main异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
		return a;
	}
	
	public void main() throws Exception {
		//查询位置为空的数据
		List<Map<String,Object>> list = queryUsermark();
		
		String device_sn;
		long load_time;
		JSONObject obj = null;
		
		int total = list.size();
		int success_number = 0;
		int empty_number = 0;
		
		long created_time;
		Object origin_time;
		Object time_added;
		
		for(int i = 0 ; i < total; i++) {
			device_sn = list.get(i).get("device_sn").toString();
			load_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(list.get(i).get("time_added").toString()).getTime();
			
			//判断装车时间是否与入库时间相差90天
			created_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(list.get(i).get("created_time").toString()).getTime();
			origin_time = list.get(i).get("origin_time");
			time_added = list.get(i).get("time_added");
			if(Math.abs(created_time-load_time) > max_time) {
				origin_time = list.get(i).get("time_added");
				load_time = created_time;
				time_added = list.get(i).get("created_time");
			}
			
			/*//查gpstraces
			try {
				obj = this.getLocationByTraces(device_sn, load_time);
			} catch (Exception e) {
				XxlJobLogger.log("[main.getLocationByTraces][exception],device_sn={0},e={1}",device_sn,ExceptionUtils.getStackTrace(e));
			}
			if(obj != null && obj.size() > 0) {
				success_number++;
				this.update(device_sn, obj.getString("province"), obj.getString("city"), obj.getString("district"), obj.getString("location"), 1, time_added, origin_time);
				continue;
			}
			
			//查gpsdrivingrecord
			try {
				obj = this.getLocationByDriving(device_sn, load_time);
			} catch (Exception e) {
				XxlJobLogger.log("[main.getLocationByDriving][exception],device_sn={0},e={1}",device_sn,ExceptionUtils.getStackTrace(e));
			}
			if(obj != null && obj.size() > 0) {
				success_number++;
				this.update(device_sn, obj.getString("province"), obj.getString("city"), obj.getString("district"), obj.getString("location"), 1, time_added, origin_time);
				continue;
			}*/
			
			//查gpspoints
			/*String result = getLastGpspoints(device_sn);
			if(StringUtils.isNotBlank(result)) {
				JSONObject json = JSONObject.parseObject(result).getJSONObject("data");
				if(json != null && json.containsKey("point")) {
					String[] point_arr = json.getString("point").split(",");
					JSONObject location_obj = this.getLocation(Double.valueOf(point_arr[0]), Double.valueOf(point_arr[1]), json.getInteger("ver"));
					if(location_obj != null && location_obj.size() > 0) {
						success_number++;
						this.update(device_sn, location_obj.getString("province"), location_obj.getString("city"), location_obj.getString("district"), location_obj.getString("formatted_address"), 2);
						continue;
					}
				}
			}*/
			empty_number++;
			/*//清除位置信息
			this.deleteLoction(device_sn);
			XxlJobLogger.log("[UpdateUsermarkLocationJob.main][empty,找不到位置信息],device_sn={0}",device_sn);*/
			//更新时间
			this.updateTime(device_sn, time_added, origin_time);
		}
		
		XxlJobLogger.log("[UpdateUsermarkLocationJob.main][统计],total={0},success_number={1},empty_number={2}",total,success_number,empty_number);
	}
	
	public JSONObject getLocationByTraces(String device_sn, long load_time) throws Exception {
		JSONObject reportData = new JSONObject();
		
		long start_at1 = -1;
		long start_at2 = -1;
		
		Map<String, Object> param = new HashMap<>();
		param.put("did", device_sn);
		param.put("time_added", load_time);
		
		//查询轨迹表（gpstraces）取经纬度(points),start_at < time_added <= end_at
		JSONArray arr1 = queryMainYundash(param, traces_query_id1);
		if(arr1 != null && arr1.size() > 0) {
			start_at1 = arr1.getJSONObject(0).getLongValue("timestamp");
			//判断轨迹时间是否与装车时间在12H内
			if(Math.abs(start_at1-load_time) > diff_time) {
				XxlJobLogger.log("[UpdateUsermarkLocationJob.getLocationByTraces]轨迹时间1与装车时间差值大于12H，device_sn={0},load_time={1},start_at1={2}",device_sn,load_time,start_at1);
				start_at1 = -1;
			}
		}
		JSONArray arr2 = queryMainYundash(param, traces_query_id2);
		if(arr2 != null && arr2.size() > 0) {
			start_at2 = arr2.getJSONObject(0).getLongValue("timestamp");
			//判断轨迹时间是否与装车时间在12H内
			if(Math.abs(start_at2-load_time) > diff_time) {
				XxlJobLogger.log("[UpdateUsermarkLocationJob.getLocationByTraces]轨迹时间2与装车时间差值大于12H，device_sn={0},load_time={1},start_at1={2}",device_sn,load_time,start_at2);
				start_at2 = -1;
			}
		}
		
		JSONArray arr = null;
		if(start_at1 == -1 && start_at2 == -1) {
			XxlJobLogger.log("[UpdateUsermarkLocationJob.getLocationByTraces]yundash查询数据为空，device_sn={0}",device_sn);
			return null;
		}else if(start_at1 == -1 && start_at2 != -1) {
			arr = arr2;
		}else if(start_at1 != -1 && start_at2 == -1) {
			arr = arr1;
		}else if(start_at1 != -1 && start_at2 != -1) {
			if(Math.abs(load_time - start_at1) < Math.abs(start_at2 - load_time)) {
				arr = arr1;
			}else {
				arr = arr2;
			}
		}
		if(arr != null && arr.size() > 0) {
			JSONObject obj = arr.getJSONObject(0);
			JSONArray points_arr = obj.getJSONArray("points");
			String point = points_arr.getString(0);
			double longitude = Double.valueOf(point.split(",")[0]);
			double latitude = Double.valueOf(point.split(",")[1]);
			int ver = obj.getInteger("ver");
			
			//调cdp获取位置信息
			JSONObject loction_json = getLocation(longitude, latitude, ver);
			if(loction_json != null && loction_json.size() > 0) {
				reportData.put("province", loction_json.getString("province"));
				reportData.put("city", loction_json.getString("city"));
				reportData.put("district", loction_json.getString("district"));
				reportData.put("location", loction_json.getString("formatted_address"));
			}
		}
		
		return reportData;
	}
	
	public JSONObject getLocationByDriving(String device_sn, long load_time) throws Exception {
		JSONObject reportData = new JSONObject();
		
		long start_at1 = -1;
		long start_at2 = -1;
		
		Map<String, Object> param = new HashMap<>();
		param.put("did", device_sn);
		param.put("time_added", load_time);
		
		//查询行程记录表（gpsdrivingrecord）取经纬度(start_point),start_at < time_added <= end_at
		JSONArray arr1 = queryMainYundash(param, driving_query_id1);
		if(arr1 != null && arr1.size() > 0) {
			start_at1 = arr1.getJSONObject(0).getLongValue("start_at");
			//判断轨迹时间是否与装车时间在12H内
			if(Math.abs(start_at1-load_time) > diff_time) {
				XxlJobLogger.log("[UpdateUsermarkLocationJob.getLocationByDriving]轨迹时间1与装车时间差值大于12H，device_sn={0},load_time={1},start_at1={2}",device_sn,load_time,start_at1);
				start_at1 = -1;
			}
		}
		JSONArray arr2 = queryMainYundash(param, driving_query_id2);
		if(arr2 != null && arr2.size() > 0) {
			start_at2 = arr2.getJSONObject(0).getLongValue("start_at");
			//判断轨迹时间是否与装车时间在12H内
			if(Math.abs(start_at2-load_time) > diff_time) {
				XxlJobLogger.log("[UpdateUsermarkLocationJob.getLocationByDriving]轨迹时间2与装车时间差值大于12H，device_sn={0},load_time={1},start_at1={2}",device_sn,load_time,start_at2);
				start_at2 = -1;
			}
		}
		
		JSONArray arr = null;
		if(start_at1 == -1 && start_at2 == -1) {
			XxlJobLogger.log("[UpdateUsermarkLocationJob.getLocationByDriving]yundash查询数据为空，device_sn={0}",device_sn);
			return null;
		}else if(start_at1 == -1 && start_at2 != -1) {
			arr = arr2;
		}else if(start_at1 != -1 && start_at2 == -1) {
			arr = arr1;
		}else if(start_at1 != -1 && start_at2 != -1) {
			if(Math.abs(load_time - start_at1) < Math.abs(start_at2 - load_time)) {
				arr = arr1;
			}else {
				arr = arr2;
			}
		}
		
		if(arr != null && arr.size() > 0) {
			JSONObject obj = arr.getJSONObject(0);
			
			String point = obj.getString("start_point");
			double longitude = Double.valueOf(point.split(",")[0]);
			double latitude = Double.valueOf(point.split(",")[1]);
			int ver = obj.getInteger("ver");
			
			//调cdp获取位置信息
			JSONObject loction_json = getLocation(longitude, latitude, ver);
			if(loction_json != null && loction_json.size() > 0) {
				reportData.put("province", loction_json.getString("province"));
				reportData.put("city", loction_json.getString("city"));
				reportData.put("district", loction_json.getString("district"));
				reportData.put("location", loction_json.getString("formatted_address"));
			}
		}
		
		return reportData;
	}
	
	/**
	 * 查询设备最后位置
	 */
	public String getLastGpspoints(String did) {
		String result_location = null;
		try {
			result_location = restTemplate.getForObject(String.format(getLastPoint_url,did), String.class);
		} catch (Exception e) {
			XxlJobLogger.log("[AddDeviceInstallCarJob.getLastGpspoints][ERROR]调ls获取设备最后经纬度失败，did={0}",did);
		}
		return result_location;
	}
	
/*	//update location_type
	public int updateLocationType() {
		String sql = "UPDATE cc_device_event_usermark SET location_type = 1 WHERE location_type = 2 AND created_time = updated_time ";
		return jdbcTemplate.update(sql);
		
	}
	
	//cc_device_event_usermark总数查询
	public int queryUsermarkCount() {
		String sql = "SELECT COUNT(*) FROM cc_device_event_usermark WHERE location_type = 2 AND id <= 192227 ";
		return jdbcTemplate.queryForObject(sql, Integer.class);
		
	}*/
	//cc_device_event_usermark did和time查询
	public List<Map<String,Object>> queryUsermark() {
		String sql = "SELECT device_sn,time_added,created_time,origin_time FROM cc_device_event_usermark where created_time >= '2020-09-15 00:00:00' and created_time < '2020-10-13 19:00:00' and ((province is null and city is null and district is null and location is null) or (province = '' and city = '' and district = '' and location = '')) ";
		return jdbcTemplate.queryForList(sql);
		
	}
	
	public int update(String device_sn,String province,String city,String district,String location,int location_type,Object time_added,Object origin_time) {
		String sql = "UPDATE cc_device_event_usermark SET location_type = ?, province = ?,city = ?,district = ?,location = ?,time_added = ?,origin_time = ? WHERE device_sn = ? ";
		return jdbcTemplate.update(sql, location_type, province, city, district, location, time_added, origin_time, device_sn);
	}
	
	public int deleteLoction(String device_sn) {
		String sql = "UPDATE cc_device_event_usermark SET origin_time = null,location_type = 1,province = '',city = '',district = '',location = '' WHERE device_sn = ? ";
		return jdbcTemplate.update(sql, device_sn);
	}
	
	public int updateTime(String device_sn,Object time_added,Object origin_time) {
		String sql = "UPDATE cc_device_event_usermark SET time_added = ?,origin_time = ?,location_type = 1 WHERE device_sn = ? ";
		return jdbcTemplate.update(sql, time_added,origin_time,device_sn);
	}
	
	/**
     * 主查询
     * @return
     */
    public JSONArray queryMainYundash (Map<String, Object> param, String query_id) throws Exception {
        String param_url = "";
        Set<String> set = param.keySet();
        for (String key:set) {
            param_url += "&p_" + key + "=" + param.get(key);
        }
        JSONObject refresh_result = null;
        String result_url = String.format(yundash_url+"/api/queries/%s/refresh?api_key=%s", query_id,yundash_api_key) + param_url;
        refresh_result = refreshYundash(result_url);
        String id = refresh_result.getJSONObject("job").getString("id");
        Integer query_result_id = getYundashJobId(yundash_url+"/api/jobs/%s?api_key=%s",id,1);
        if (query_result_id == -1) {
            XxlJobLogger.log("yundash查询超时");
            return null;
        }
        JSONArray doc_count_arr = getYundashQueryResultId(yundash_url+"/api/query_results/%s?api_key=%s",query_result_id);
        return doc_count_arr;
    }
	
    /**
     * 刷新yundash
     */
    public JSONObject refreshYundash(String url){
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        String result = restTemplate.postForObject(url, headers, String.class);
        return JSONObject.parseObject(result);
    }
    
    /**
     * 获取yundash的jobid 
     */
    public int getYundashJobId(String url, String id, int index) throws InterruptedException{
        String result = restTemplate.getForObject(String.format(url, id,yundash_api_key), String.class);
        JSONObject job_result = JSONObject.parseObject(result);
        if (null != job_result.getJSONObject("job").getInteger("query_result_id")) {
            return job_result.getJSONObject("job").getInteger("query_result_id");
        } else {
            if (index > 200) {
                return -1;
            }
            Thread.sleep(300);
            index++;
            return getYundashJobId(url, id, index);
        }
    }
    
    /**
     * 根据查询id获取yundash数据 
     */
    public JSONArray getYundashQueryResultId(String url,Integer query_result_id) {
        String result = restTemplate.getForObject(String.format(url, query_result_id,yundash_api_key), String.class);
        JSONObject res = JSONObject.parseObject(result);
        return res.getJSONObject("query_result").getJSONObject("data").getJSONArray("rows");
    }
    
    //yundash查询行程记录表（gpsdrivingrecord）
	/*public JSONArray findYundashData(String did,String time) {
		String yds_query_url = String.format(yundash_url+"/api/queries/%s/results.json?api_key=%s", query_id1,yundash_api_key);
		yds_query_url = yds_query_url + "&did=" + did + "&time=" + time;
		String yds_result_str = restTemplate.getForObject(yds_query_url, String.class);
		JSONObject yds_result_json = JSONObject.parseObject(yds_result_str);
		JSONArray yds_result = yds_result_json.getJSONObject("query_result").getJSONObject("data").getJSONArray("rows");
		return yds_result;
	}*/
	
	/**
	 * 根据经纬度调cdp的接口获取地址详情
	 */
	public JSONObject getLocation(double longitude, double latitude, int ver) {
		JSONObject  resut = new JSONObject();
		String result_location = null;
		try {
			result_location = restTemplate.getForObject(String.format(getLocation_url,longitude,latitude,(ver==1?"bd09ll":ver==2?"gcj02ll":"")), String.class);
		} catch (Exception e) {
			XxlJobLogger.log("[UpdateUsermarkLocationJob.getLocation][ERROR]根据经纬度调cdp接口获取地理位置异常，longitude={0},latitude={1},result_location={2}",
					longitude,latitude,result_location);
		}
		JSONObject json = JSONObject.parseObject(result_location);
		if(json != null && json.containsKey("code") && json.getInteger("code") == 0) {
			JSONObject  data = json.getJSONObject("data");
			resut.put("formatted_address", data.getString("formatted_address"));
			/*resut.put("sematic_description", data.getString("sematic_description"));
			resut.put("cityCode", data.getInteger("cityCode"));*/
			
			JSONObject  addressComponent = json.getJSONObject("data").getJSONObject("addressComponent");
			resut.put("province", addressComponent.getString("province"));
			resut.put("city", addressComponent.getString("city"));
			resut.put("district", addressComponent.getString("district"));
			/*resut.put("street", addressComponent.getString("street"));
			resut.put("city_level", addressComponent.getInteger("city_level"));
			resut.put("country", addressComponent.getString("country"));
			resut.put("adcode", addressComponent.getString("adcode"));
			resut.put("country_code", addressComponent.getInteger("country_code"));*/
		} else {
			XxlJobLogger.log("[UpdateUsermarkLocationJob.getLocation][WARN]根据经纬度调cdp接口获取地理位置失败，longitude={0},latitude={1},result_location={2}",
					longitude,latitude,result_location);
		} 
		return resut;
	}
	
}
