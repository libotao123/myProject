import cn.yunovo.job.core.glue.GlueTypeEnum;

public class Test {
	
	public static void main(String[] args) {
		System.out.println(GlueTypeEnum.match("GLUE_PHP").isScript() +"  "+ GlueTypeEnum.match("GLUE_PHP")==null);
	}

}
