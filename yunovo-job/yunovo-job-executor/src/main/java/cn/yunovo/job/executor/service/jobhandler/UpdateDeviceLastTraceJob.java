package cn.yunovo.job.executor.service.jobhandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="updateDeviceLastTraceJob")
@Component
public class UpdateDeviceLastTraceJob extends IJobHandler {
	
	String getLastPoint_url = "http://192.168.3.241:8080/rest/api/trace/getLastPointBycreated_at?startTime=%s&pageSize=%s";
	
	/**根据经纬度获取地理位置*/
	String getLocation_url = "http://192.168.3.240:7000/rest/api/lbs/getLocation?longitude=%s&latitude=%s&coordtype=%s";
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private RestTemplate restTemplate;
  
	@Override
	public ReturnT<String> execute(String param) throws Exception {
		updateDeviceLastTrace(null,1541088000000l,1000);
		/*updateDeviceLastTrace(null,initDateByDay(-300,0,0,0).getTime(),1000);*/
		
		return new ReturnT<String>(202,"成功");
	}
	
	public void updateDeviceLastTrace (String token_device_sn,Long startTime,int pageSize) throws Exception{
		JSONArray result = batchGetDeviceTrace(token_device_sn, startTime, pageSize);
		if (null == result) {
			return ;
		}
		for (int i=0;i<result.size();i++) {
			JSONObject jo = result.getJSONObject(i);
			String[] lng_lat = jo.getString("point").split(",");
			saveDeviceLastTraceDao(jo.getString("did"),jo.getLong("timestamp"),Double.valueOf(lng_lat[0]),Double.valueOf(lng_lat[1]),jo.getInteger("ver"), jo.getLong("created_at"),jo.getString("gpsdata"));
		}
		if (result.size() < pageSize) {
			return;
		}
		updateDeviceLastTrace(result.getJSONObject(result.size()-1).getString("did"),startTime,pageSize);
	}
	
	/**
	 * 批量更新设备地址,根据点与上次的点进行比较,如果距离比较远,则调百度api,批量更新,否则不更新;deviceId不存在的insert,存在的update
	 * bd09ll（百度经纬度坐标）、gcj02ll（国测局经纬度坐标）、wgs84ll（ GPS经纬度）
	 * param: ver(1为百度坐标系,2为火星)
	 */
	public void saveDeviceLastTraceDao (String device_sn, Long timestamp, double longitude, double latitude, int ver, Long created_at, String gpsdata) throws Exception{
		if (null == device_sn) {
			return;
		}
		Map<String, Object> params = new HashMap<>();
		String sql = "SELECT device_sn,longitude,latitude from cc_device_last_location WHERE device_sn= ( :device_sn)";
		params.put("device_sn", device_sn);
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		List<JSONObject> result = namedParameterJdbcTemplate.query(sql,params,new RowMapper(){
			@Override
		     public JSONObject mapRow(ResultSet rs, int rowNum) throws SQLException {
				JSONObject json = new JSONObject();
				json.put("longitude", rs.getDouble("longitude"));
				json.put("latitude", rs.getDouble("latitude"));
				return json;
			}
		});
		if (result.isEmpty()) {
			JSONObject Loction_json = getLocation(longitude, latitude, ver);
			/*if(null == Loction_json) {
				XxlJobLogger.log("[UpdateDeviceLastTraceJob.getLocation][ERROR]设备sn={0}更新失败,原因调cdp中调百度api接口获取设备位置失败");
				return;
			}*/
			insertDeviceLocation(longitude, latitude, device_sn, Loction_json.getString("province"), 
					Loction_json.getString("city"),Loction_json.getString("district"),Loction_json.getString("street"),
					Loction_json.getString("formatted_address"),Loction_json.getString("sematic_description"),Loction_json.getInteger("cityCode"),
					Loction_json.getInteger("city_level"), Loction_json.getString("country"),Loction_json.getString("adcode"),
					Loction_json.getInteger("country_code"), timestamp,ver, created_at, gpsdata);
		} else {
			JSONObject device_json = result.get(0);
			double distance = getDistance(latitude,longitude,device_json.getDouble("latitude"),device_json.getDouble("longitude"));
			if (distance > -1) {
				JSONObject Loction_json = getLocation(longitude, latitude, ver);
				updateDeviceLocation(longitude, latitude, device_sn, Loction_json.getString("province"), 
						Loction_json.getString("city"),Loction_json.getString("district"),Loction_json.getString("street"),
						Loction_json.getString("formatted_address"),Loction_json.getString("sematic_description"),Loction_json.getInteger("cityCode"),
						Loction_json.getInteger("city_level"), Loction_json.getString("country"),Loction_json.getString("adcode"),
						Loction_json.getInteger("country_code"), timestamp,ver, created_at, gpsdata);
			}
			
			
		}
	}
	
	/**
	 * 根据经纬度调cdp的接口获取地址详情
	 */
	public JSONObject getLocation(double longitude, double latitude, int ver) {
		JSONObject  resut = new JSONObject();
		String result_location = restTemplate.getForObject(String.format(getLocation_url,longitude,latitude,(ver==1?"bd09ll":ver==2?"gcj02ll":"")), String.class);
		JSONObject json = JSONObject.parseObject(result_location);
		if(json.containsKey("code") && json.getInteger("code") == 0) {
			JSONObject  data = json.getJSONObject("data");
			resut.put("formatted_address", data.getString("formatted_address"));
			resut.put("sematic_description", data.getString("sematic_description"));
			resut.put("cityCode", data.getInteger("cityCode"));
			
			JSONObject  addressComponent = json.getJSONObject("data").getJSONObject("addressComponent");
			resut.put("province", addressComponent.getString("province"));
			resut.put("city", addressComponent.getString("city"));
			resut.put("district", addressComponent.getString("district"));
			resut.put("street", addressComponent.getString("street"));
			
			resut.put("city_level", addressComponent.getInteger("city_level"));
			resut.put("country", addressComponent.getString("country"));
			resut.put("adcode", addressComponent.getString("adcode"));
			resut.put("country_code", addressComponent.getInteger("country_code"));
		} else {
			XxlJobLogger.log("[UpdateDeviceLastTraceJob.getLocation][ERROR]根据经纬度调cdp接口获取地理位置失败，longitude={0},latitude={1},result_location={2}",
					longitude,latitude,result_location);
		} 
		return resut;
	}
	
	public int insertDeviceLocation( double longitude, double latitude, String device_sn, String province, String city, String district, String street,String formatted_address,String sematic_description,int cityCode,int city_level,String country,String adcode,int country_code, Long timestamp,int ver,Long created_at, String gpsdata) throws SQLException {
		List<Object> params = new ArrayList<>();
		String sql = "INSERT INTO cc_device_last_location(longitude,latitude,ver,time_stamp,province,city,district,street, formatted_address, sematic_description, cityCode, city_level, country, adcode, country_code,update_datetime,device_sn,created_at, gpsdata) VALUES(?, ?, ?, ?,?,?,?,?,  ?,?,?,?,?,?,?,now(),?,?,?)";
		params.add(longitude);
		params.add(latitude);
		params.add(ver);
		params.add(new Date(timestamp));
		params.add(province);
		params.add(city);
      	params.add(district);
      	params.add(street);
      	params.add(formatted_address);
      	params.add(sematic_description);
      	params.add(cityCode);
      	params.add(city_level);
      	params.add(country);
      	params.add(adcode);
      	params.add(country_code);
      	params.add(device_sn);
      	params.add(created_at == null ? null : new Date(created_at));
      	params.add(gpsdata);
		return jdbcTemplate.update(sql,params.toArray());
	}
	
	public int updateDeviceLocation( double longitude, double latitude, String device_sn, String province, String city, String district, String street, String formatted_address,String sematic_description,int cityCode,int city_level,String country,String adcode,int country_code,Long timestamp,int ver,Long created_at, String gpsdata) throws SQLException {
		List<Object> params = new ArrayList<>();
		String sql = "UPDATE cc_device_last_location SET longitude=?,latitude=?,ver=?,time_stamp=?,province=?,city=?, district = ?, street = ?, formatted_address = ?, sematic_description =?, cityCode = ?, city_level = ?, country = ?, adcode = ?, country_code = ?, update_datetime = now(), created_at = ?, gpsdata = ? WHERE device_sn = ?";
		params.add(longitude);
		params.add(latitude);
		params.add(ver);
		params.add(new Date(timestamp));
		params.add(province);
		params.add(city);
      	params.add(district);
      	params.add(street);
      	params.add(formatted_address);
      	params.add(sematic_description);
      	params.add(cityCode);
      	params.add(city_level);
      	params.add(country);
      	params.add(adcode);
      	params.add(country_code);
      	params.add(created_at == null ? null : new Date(created_at));
      	params.add(gpsdata);
      	params.add(device_sn);
		return jdbcTemplate.update(sql, params.toArray());
	}
	
	/**
     * 坐标之间的距离
     * @return 单位米
     */
    public double getDistance(double lat1, double lng1, double lat2, double lng2) {
        lat1 = Math.toRadians(lat1);
        lng1 = Math.toRadians(lng1);
        lat2 = Math.toRadians(lat2);
        lng2 = Math.toRadians(lng2);
        double d1 = Math.abs(lat1 - lat2);
        double d2 = Math.abs(lng1 - lng2);
        double p = Math.pow(Math.sin(d1 / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(d2 / 2), 2);
        return 6378137 * 2 * Math.asin(Math.sqrt(p));
    }
    
    /**
	 * 获得时间
	 * @return
	 */
	public static Date initDateByDay(int month,int hour,int minute,int second){
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, month);
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, second);
		return calendar.getTime();
	}

    public JSONArray batchGetDeviceTrace(String token_did,Long startTime,int pageSize){
    	String url = String.format(getLastPoint_url, startTime, pageSize);
    	if (null != token_did) {
    		url += ("&token_did="+token_did);
    	}
    	ResponseEntity<String> result = restTemplate.getForEntity(url, String.class);
		JSONObject jsonObject= JSON.parseObject(result.getBody());
		JSONArray jsonArray = jsonObject.getJSONArray("data");
		return jsonArray;
	}

}
