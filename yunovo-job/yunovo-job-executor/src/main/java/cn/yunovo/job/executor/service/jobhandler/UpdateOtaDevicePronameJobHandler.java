package cn.yunovo.job.executor.service.jobhandler;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="update_ota_device_proname")
@Component
/*
 * 更新ota升级记录产品型号
 * 
 * 
 */
public class UpdateOtaDevicePronameJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(UpdateOtaDevicePronameJobHandler.class);
	
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
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
            logger.error("[UpdateOtaDevicePronameJobHandler.execute][execute异常],e={}",ExceptionUtils.getStackTrace(e));
            XxlJobLogger.log("[UpdateOtaDevicePronameJobHandler.execute][execute异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
		return a;
    }

    public void main(){
    	
    	//查询总数
    	int count = queryOtaDeviceCount();
    	int pageSize = 2000;
    	int pageCount = count%pageSize == 0 ? count/pageSize : count/pageSize + 1;
    	
    	logger.error("[UpdateOtaDevicePronameJobHandler.main],count={},pageCount={}",count,pageCount);
        XxlJobLogger.log("[UpdateOtaDevicePronameJobHandler.main],count={0},pageCount={1}",count,pageCount);
    	
    	int limit_start;
    	//分页查询数据
    	String temp_proname = "";
    	String temp_org_code = null;
    	for(int i = 0;i < pageCount;i++) {
    		limit_start = i*pageSize;
    		List<Map<String,Object>> page_list = queryPage(limit_start,pageSize);
    		
    		//解析产品型号
    		for (Map<String, Object> map : page_list) {
    			temp_org_code = map.get("org_code").toString();
    			temp_proname = temp_org_code.substring(temp_org_code.lastIndexOf("-")+1, temp_org_code.length());
    			map.put("pro_name", temp_proname);
			}
    		
    		//更新产品型号
    		try {
				updateProname(page_list);
			} catch (Exception e) {
				logger.error("[UpdateOtaDevicePronameJobHandler.updateProname],e={}",JSONObject.toJSONString(e));
		        XxlJobLogger.log("[UpdateOtaDevicePronameJobHandler.updateProname],e={0}",JSONObject.toJSONString(e));
			}
    		
    	}
    	
    }

	public int queryOtaDeviceCount(){
		String sql = "SELECT COUNT(*) FROM cc_ota_device WHERE org_code LIKE '%-%' ";
		return jdbcTemplate.queryForObject(sql, Integer.class);
	}
    	
	public List<Map<String,Object>> queryPage(int start,int end){
		String sql = "SELECT otad_id,org_code FROM cc_ota_device WHERE org_code LIKE '%-%' ORDER BY otad_id LIMIT ?,? ";
		return jdbcTemplate.queryForList(sql, start,end);
	}
	
	public int[] updateProname(List<Map<String,Object>> list){
		String sql = "UPDATE cc_ota_device SET pro_name = ? WHERE otad_id = ? ";
		
		int i = 0;
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setString(1, list.get(i).get("pro_name").toString());
				ps.setString(2, list.get(i).get("otad_id").toString());
				i++;
			}

			@Override
			public int getBatchSize() {
				return list.size();
			}
			
		});
		return null;
	}
	
	public String substring(String org_code) {
		return org_code.substring(org_code.lastIndexOf("-")+1, org_code.length());
	}
	
}
