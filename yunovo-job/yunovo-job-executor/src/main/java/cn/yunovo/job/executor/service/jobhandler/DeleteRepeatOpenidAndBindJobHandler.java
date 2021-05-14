package cn.yunovo.job.executor.service.jobhandler;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="delete_repeat_openid_bind")
@Component
/*
 * 删除重复的openid
 * 
 * 
 */
public class DeleteRepeatOpenidAndBindJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(DeleteRepeatOpenidAndBindJobHandler.class);
	
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public ReturnT<String> execute(String wx_id) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
		try {
			main(wx_id);
			a.setCode(200);
			a.setMsg("success");
		} catch (Exception e) {
			e.printStackTrace();
            a.setCode(500);
            logger.error("[DeleteRepeatOpenidAndBindJobHandler.execute][execute异常],e={}",ExceptionUtils.getStackTrace(e));
            XxlJobLogger.log("[DeleteRepeatOpenidAndBindJobHandler.execute][execute异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
		return a;
    }

    public void main(String wx_id_string){
    	int count = 0;
    	Long wx_id = Long.valueOf(wx_id_string);
    	logger.error("[DeleteRepeatOpenidAndBindJobHandler.main][wx_id],wx_id={}",wx_id);
        XxlJobLogger.log("[DeleteRepeatOpenidAndBindJobHandler.main][wx_id],wx_id={0}",wx_id);
    	//根据wx_id删除cc_customer_wx表数据
    	count = deleteCustomerWxByWxid(wx_id);
    	logger.error("[DeleteRepeatOpenidAndBindJobHandler.main][deleteCustomerWxByWxid结果],count={}",count);
        XxlJobLogger.log("[DeleteRepeatOpenidAndBindJobHandler.main][deleteCustomerWxByWxid结果],count={0}",count);
    	//根据wx_id删除cc_device_bind里的数据
    	count = updateDeviceBindByWxid(wx_id);
    	logger.error("[DeleteRepeatOpenidAndBindJobHandler.main][updateDeviceBindByWxid结果],count={}",count);
        XxlJobLogger.log("[DeleteRepeatOpenidAndBindJobHandler.main][updateDeviceBindByWxid结果],count={0}",count);
    }	
    	
	public int deleteCustomerWxByWxid(Long wx_id){
		String sql = "DELETE FROM cc_customer_wx WHERE wx_id = ? ";
		return jdbcTemplate.update(sql, wx_id);
	}
	
	public int updateDeviceBindByWxid(Long wx_id){
		String sql = "UPDATE cc_device_bind SET `status` = 0 WHERE wx_id = ? ";
		return jdbcTemplate.update(sql, wx_id);
	}
	
	
}
