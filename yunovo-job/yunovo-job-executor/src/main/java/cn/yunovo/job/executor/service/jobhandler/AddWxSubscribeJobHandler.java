package cn.yunovo.job.executor.service.jobhandler;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="add_wx_subscribe")
@Component
/*
 * add_wx_subscribe
 * 
 * 
 */
public class AddWxSubscribeJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(AddWxSubscribeJobHandler.class);
	
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
    	
    	String openids = "oYwc71BdOnKlkO0THJ5q5G40N0Zk,oYwc71BdOnKlkO0THJ5q5G40N0Zk1";
    	String[] arr_openid = openids.split(",");
    	logger.error("[UpdateOtaDevicePronameJobHandler.execute],size={}",arr_openid.length);
        XxlJobLogger.log("[UpdateOtaDevicePronameJobHandler.execute],size={0}",arr_openid.length);
    	//批量插入
    	batchInsert(arr_openid);
    }
	
	public int[] batchInsert(String[] arr_openid){
		String sql = "INSERT INTO cc_wx_subscribe (open_id,wx_domain,`event`,create_time) SELECT ?,'wx007.yunovo.cn','subscribe','2020-05-14 01:00:00' FROM DUAL WHERE NOT EXISTS (SELECT id FROM cc_wx_subscribe WHERE wx_domain = 'wx007.yunovo.cn' AND open_id = ? LIMIT 1) ";
		
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setString(1, arr_openid[i]);
				ps.setString(2, arr_openid[i]);
				i++;
			}

			@Override
			public int getBatchSize() {
				return arr_openid.length;
			}
			
		});
		return null;
	}	
	
}
