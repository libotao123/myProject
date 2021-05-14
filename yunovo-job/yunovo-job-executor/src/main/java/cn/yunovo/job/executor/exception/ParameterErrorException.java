package cn.yunovo.job.executor.exception;

import java.io.IOException;

public class ParameterErrorException extends IOException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public  ParameterErrorException(String errMsg) {
		super(errMsg);
	}
	
}
