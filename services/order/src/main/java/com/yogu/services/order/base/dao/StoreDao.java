package com.yogu.services.order.base.dao;

import com.yogu.commons.datasource.annocation.TheDataDao;
import com.yogu.services.order.base.entry.StorePO;

import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * t_store 表的操作接口
 * 
 * @author JFan 2015-07-13
 */
@TheDataDao
public interface StoreDao {

	/**
	 * 保存数据
	 * 
	 * @param po 对象
	 */
	public void save(StorePO po);

	// /**
	// * 修改数据，以主键更新
	// *
	// * @param po - 要更新的数据
	// * @return 更新的行数
	// */
	// public int update(OrderDetailPO po);

	/**
	 * 根据主键读取记录
	 */
	public StorePO getById(@Param("storeId") long storeId);

	/**
	 * 根据商户编号获取商户信息<br>
	 * 如果这个商户编号有多跳信息，返回最早的
	 * 
	 * @param storeNo - 商户编号
	 */
	public StorePO getByStoreMobile(@Param("storeMobile") String storeMobile);

}
