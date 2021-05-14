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

@JobHandler(value="update_activity_info_job_lbt")
@Component

/*
 * 微信公众号活动信息更新
 * 
 * 
 */
public class UpdateActivityInfoJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(UpdateActivityInfoJobHandler.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
		try {
			updateTemplateMsg();
			a.setCode(200);
			a.setMsg("success");
		} catch (Exception e) {
			e.printStackTrace();
            a.setCode(500);
            logger.error("[UpdateTemplateMsgJobHandler26_2030.updateTemplateMsg][更新活动信息异常]");
            XxlJobLogger.log("[UpdateTemplateMsgJobHandler26_2030.updateTemplateMsg][更新活动信息异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
		
		return a;
    }
	
	//更新模板消息
	public void updateTemplateMsg() throws Exception {
		String sql1 = "UPDATE cc_wx_activity_info SET activity_start_time = '2019-09-10 00:00:00',activity_end_time = '2019-09-13 23:59:59' WHERE activity_code = 'message_ysj';";
		jdbcTemplate.update(sql1);
	}
	
}
