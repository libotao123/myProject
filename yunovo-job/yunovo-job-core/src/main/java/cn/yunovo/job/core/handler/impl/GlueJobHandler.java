package cn.yunovo.job.core.handler.impl;

import java.util.Date;

import org.apache.http.client.utils.DateUtils;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

/**
 * glue job handler
 * @author xuxueli 2016-5-19 21:05:45
 */
public class GlueJobHandler extends IJobHandler {

	private long glueUpdatetime;
	private IJobHandler jobHandler;
	public GlueJobHandler(IJobHandler jobHandler, long glueUpdatetime) {
		this.jobHandler = jobHandler;
		this.glueUpdatetime = glueUpdatetime;
	}
	public long getGlueUpdatetime() {
		return glueUpdatetime;
	}
	
	

	@Override
	public void init() {
		XxlJobLogger.log("----------- glue.init:"+ DateUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss") +" -----------");
		jobHandler.init();
	}
	@Override
	public void destroy() {
		//super.destroy();
		XxlJobLogger.log("----------- glue.destroy:"+ DateUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss") +" -----------");
		jobHandler.destroy(); 
	}
	@Override
	public ReturnT<String> execute(String param) throws Exception {
		XxlJobLogger.log("----------- glue.version:"+ glueUpdatetime +" -----------");
		return jobHandler.execute(param);
	}

}
