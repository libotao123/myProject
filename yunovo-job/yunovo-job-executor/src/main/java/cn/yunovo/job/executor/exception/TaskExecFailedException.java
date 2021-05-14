package cn.yunovo.job.executor.exception;

import java.io.IOException;

import javax.sql.rowset.JoinRowSet;

import com.alibaba.fastjson.JSONObject;

public class TaskExecFailedException extends IOException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Integer count;
	
	private JSONObject data;
	
	public TaskExecFailedException(Integer count, JSONObject data, String errorMsg) {
		super(errorMsg);
		this.count = count == null ? 0:count;
		this.data = data;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public JSONObject getData() {
		return data;
	}

	public void setData(JSONObject data) {
		this.data = data;
	}



	
	
	
}
