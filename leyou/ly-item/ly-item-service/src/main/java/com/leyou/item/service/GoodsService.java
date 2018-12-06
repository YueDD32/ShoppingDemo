package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.dto.CartDTO;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper detailMapper;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, Boolean saleable, String key) {
        // 1 分页
        PageHelper.startPage(page, rows);
        // 2 过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        // 2.1 关键字过滤
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }
        // 2.2 上下架过滤
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);
        }
        // 2.3 过滤被逻辑删除的
        criteria.andEqualTo("valid", true);

        // 3 排序（按更新时间）
        example.setOrderByClause("last_update_time DESC");
        // 4 查询结果
        List<Spu> spus = spuMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(spus)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }

        // 5 处理分类和品牌的名称
        handleCategoryAndBrandNames(spus);

        // 6 封装结果并返回
        PageInfo<Spu> info = new PageInfo<>(spus);
        return new PageResult<>(info.getTotal(), spus);
    }

    private void handleCategoryAndBrandNames(List<Spu> spus) {
        for (Spu spu : spus) {
            // 查询分类
            String cname = categoryService.queryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                    .stream() // 把集合转为流
                    .map(Category::getName) // 把Category对象流转为name的流
                    .collect(Collectors.joining("/"));// 把流中的数据以'/'进行拼接
            // 设置分类名称
            spu.setCname(cname);

            // 设置品牌名称
            Brand brand = brandService.queryById(spu.getBrandId());
            spu.setBname(brand.getName());
        }
    }

    @Transactional
    public void saveGoods(Spu spu) {
        // 1 新增spu
        spu.setId(null);
        spu.setSaleable(true);
        spu.setValid(true);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        int count = spuMapper.insertSelective(spu);
        if(count != 1){
            throw new LyException(ExceptionEnum.INSERT_GOODS_ERROR);
        }
        Long spuId = spu.getId();
        // 2 新增spuDetail
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spuId);
        count = detailMapper.insertSelective(spuDetail);
        if(count != 1){
            throw new LyException(ExceptionEnum.INSERT_GOODS_ERROR);
        }
        // 新增sku和stock
        saveSkuAndStock(spu);

        // 发送消息到mq
        amqpTemplate.convertAndSend("item.insert", spu.getId());
    }

    private void saveSkuAndStock(Spu spu) {
        int count;// 3 新增sku
        List<Sku> skus = spu.getSkus();
        for (Sku sku : skus) {
            sku.setSpuId(spu.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            // 新增sku
            count = skuMapper.insert(sku);
            if(count != 1){
                throw new LyException(ExceptionEnum.INSERT_GOODS_ERROR);
            }
            // 4 新增stock
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            count = stockMapper.insert(stock);
            if(count != 1){
                throw new LyException(ExceptionEnum.INSERT_GOODS_ERROR);
            }
        }
    }

    public SpuDetail queryDetailBySpuId(Long spuId) {
        SpuDetail detail = detailMapper.selectByPrimaryKey(spuId);
        if (detail == null) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return detail;
    }

    public List<Sku> querySkuListBySpuId(Long spuId) {
        // 查询sku
        Sku r = new Sku();
        r.setSpuId(spuId);
        List<Sku> skus = skuMapper.select(r);
        if (CollectionUtils.isEmpty(skus)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }

        // 查询sku下的库存
        List<Long> ids = skus.stream().map(Sku::getId).collect(Collectors.toList());

        // 填充stock到sku
        fillSkuByStock(skus, ids);

//        for (Sku sku : skus) {
//            Stock stock = stockMapper.selectByPrimaryKey(sku.getId());
//            sku.setStock(stock.getStock());
//        }
        return skus;
    }

    private void fillSkuByStock(List<Sku> skus, List<Long> ids) {
        List<Stock> stocks = stockMapper.selectByIdList(ids);
        if(stocks == null || stocks.size() < skus.size()){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        // 把stock的集合，变成一个map，key是skuId，其值是stock
        Map<Long, Integer> map = stocks.stream().collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        for (Sku sku : skus) {
            sku.setStock(map.get(sku.getId()));
        }
    }

    @Transactional
    public void updateGoods(Spu spu) {
        Long spuId = spu.getId();
        if (spuId == null) {
            throw new LyException(ExceptionEnum.SPU_ID_CANNOT_BE_NULL);
        }
        // 1 删除sku
        Sku r = new Sku();
        r.setSpuId(spuId);
        // 查询sku
        List<Sku> skus = skuMapper.select(r);
        int count = 0;
        if(!CollectionUtils.isEmpty(skus)) {
            // 删除sku
            count = skuMapper.delete(r);
            if (count != skus.size()) {
                throw new LyException(ExceptionEnum.UPDATE_GOODS_ERROR);
            }

            // 2 删除stock
            List<Long> idList = skus.stream().map(Sku::getId).collect(Collectors.toList());
            count = stockMapper.deleteByIdList(idList);
            if (count != skus.size()) {
                throw new LyException(ExceptionEnum.UPDATE_GOODS_ERROR);
            }
        }
        // 3 修改spu
        spu.setSaleable(null);
        spu.setValid(null);
        spu.setCreateTime(null);
        spu.setLastUpdateTime(new Date());
        count = spuMapper.updateByPrimaryKeySelective(spu);
        if (count != 1) {
            throw new LyException(ExceptionEnum.UPDATE_GOODS_ERROR);
        }

        // 4 修改spuDetail
        count = detailMapper.updateByPrimaryKeySelective(spu.getSpuDetail());
        if (count != 1) {
            throw new LyException(ExceptionEnum.UPDATE_GOODS_ERROR);
        }
        // 5 新增sku和stock
        saveSkuAndStock(spu);

        // 发送消息到mq
        amqpTemplate.convertAndSend("item.update", spu.getId());
    }

    public Spu querySpuById(Long id) {
        // 查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }

        // 查询skus
        List<Sku> skuList = querySkuListBySpuId(id);
        spu.setSkus(skuList);

        // 查询detail
        SpuDetail spuDetail = queryDetailBySpuId(id);
        spu.setSpuDetail(spuDetail);
        return spu;
    }

    public List<Sku> querySkuListByIds(List<Long> ids) {
        // 查询Sku
        List<Sku> skuList = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(skuList)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        // 填充stock到sku
        fillSkuByStock(skuList, ids);
        return skuList;
    }

    @Transactional
    public void decreaseStock(List<CartDTO> cartDTOS) {
        // 遍历商品
        for (CartDTO cartDTO : cartDTOS) {
            int count = stockMapper.decreaseStock(cartDTO.getSkuId(), cartDTO.getNum());
            if(count != 1){
                throw new LyException(ExceptionEnum.STOCK_NOT_ENOUGH);
            }
        }
    }
}
