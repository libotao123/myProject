package cn.yunovo.job.executor.service.jobhandler;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

public class DeleteDataJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(DeleteDataJobHandler.class);
	
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
    	String sql = "UPDATE cc_wx_configuration_info SET set_val = 'Hello，我是达客出行，您的随身车管家。\r\n\r\n您还差一步就完成设备绑定了。\r\n\r\n<a href=\'%s\'>【戳我扫码绑定】</a>领取免费流量，让爱车实时在线。\r\n\r\n如有问题，回复【人工客服】将有专属客服为您答疑解惑。' WHERE set_key = 'SUBSCRIBE_REPLY' ";
		int result = jdbcTemplate.update(sql);
		logger.error("[UpdateOtaDevicePronameJobHandler.result],result={}",result);
        XxlJobLogger.log("[UpdateOtaDevicePronameJobHandler.result],result={0}",result);
    	
    }

}
