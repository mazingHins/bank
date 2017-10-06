package com.yogu.services.order.resource.base;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.yogu.commons.utils.StringUtils;
import com.yogu.commons.utils.VOUtil;
import com.yogu.core.web.RestResult;
import com.yogu.core.web.ResultCode;
import com.yogu.services.order.base.dto.Store;
import com.yogu.services.order.base.service.StoreService;
import com.yogu.services.order.resource.vo.StoreVO;

/**
 * 商户相关api接口
 *
 */
@Named
@Path("p")
@Singleton
@Produces("application/json;charset=UTF-8")
public class StoreResource {
	
	@Inject
	private StoreService storeService;
	
	/**
	 * 通过商户编号获取商户信息
	 * 
	 * @author qiujie 
	 * @date 2017年9月24日 上午9:52:42 
	 */
	@GET
	@Path("v1/get")
	public RestResult<List<StoreVO>> get(@QueryParam("storeMobile") String storeMobile) {
		
		if(StringUtils.isBlank(storeMobile)){
			return new RestResult<>(ResultCode.PARAMETER_ERROR, "商户编号不能为空");
		}
		
		Store store = storeService.getByStoreMobile(storeMobile);
		if(null == store){
			return new RestResult<>(ResultCode.PARAMETER_ERROR, "商户不存在，请重新确认");
		}
		
		return new RestResult<>(Arrays.asList(VOUtil.from(store, StoreVO.class)));
	}
	
	
}
