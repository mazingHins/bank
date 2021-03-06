/**
 * 
 */
package com.mazing.test.jmeter.store;

import com.mazing.test.core.ApiReq;
import com.mazing.test.core.HttpRequestBuild;
import com.mazing.test.core.KeyValue;
import com.mazing.test.jmeter.AbstractJavaPerf;
import com.mazing.test.runtime.Config;

/**
 * 搜索餐厅 <br>
 * 压力测试
 *
 * @author JFan 2015年12月22日 下午9:08:35
 */
public class SearchStoreResourcePerTest extends AbstractJavaPerf {

	private ApiReq<String> req;
	private Integer[] pages = new Integer[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 4, 5, 6 };
	private Integer[] sizes = new Integer[] { 20, 20, 20, 20, 20, 20, 20, 20, 20, 21, 21, 21, 22, 23, 30, 40, 50 };

	public SearchStoreResourcePerTest() {
		super(false, true);
		collectStoreId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initial() {
		Config.giveRandomByOriginal("storeSearch");
		req = new HttpRequestBuild().build(storeHost + "/p/v1/search/store");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String call(KeyValue<String, String> ut) {
		req.setGet("query", Config.giveRandomByOriginal("storeSearch"));
		req.setGet("pageIndex", random(pages).toString());
		req.setGet("pageSize", random(sizes).toString());

		if (null != ut)
			req.userToken2Get(ut.getKey(), ut.getValue());
		else
			req.clearUserToken();

		return req.doGet();
	}

}
