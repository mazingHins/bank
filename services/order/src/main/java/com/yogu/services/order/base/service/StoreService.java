package com.yogu.services.order.base.service;

import com.yogu.services.order.base.dto.Store;

/**
 * 商户信息表 <br>
 * 的操作接口
 * 
 * @author JFan 2015-07-17
 */
public interface StoreService {

	/**
	 * 保存数据
	 * 
	 * @param dto对象
	 */
	public void save(Store dto);

	/**
	 * 根据商户编号获取商户信息<br>
	 * 如果这个商户编号有多跳信息，返回最早的
	 * 
	 * @author qiujie
	 * @date 2019年09月24日 上午09:02:47
	 * 
	 * @param storeNo - 商户编号（必传，不传返回null）
	 * @return 商户信息，若无，返回null
	 */
	public Store getByStoreMobile(String storeMobile);


}
