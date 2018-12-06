package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.client.BrandClient;
import com.leyou.item.client.CategoryClient;
import com.leyou.item.client.GoodsClient;
import com.leyou.item.client.SpecClient;
import com.leyou.item.pojo.*;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private SpecClient specClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private GoodsRepository goodsRepository;

    /**
     * 接收一个Spu，返回一个Goods
     */
    public Goods buildGoods(Spu spu){
        Long spuId = spu.getId();
        // 查询分类
        String categoryNames = categoryClient.queryByIdList(
                Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                .stream().map(Category::getName)
                .collect(Collectors.joining(" "));
        // 查询品牌
        Brand brand = brandClient.queryById(spu.getBrandId());
        // 所有搜索字段拼接，标题、分类、品牌
        String all = spu.getTitle() + categoryNames + brand.getName();//TODO 规格参数

        // 查询sku
        List<Sku> skuList = goodsClient.querySkuListBySpuId(spuId);

        // 准备一个List，里面是Map，用Map代替Sku对象
        List<Map<String, Object>> skuMap = new ArrayList<>();
        skuList.forEach(sku -> {
            Map<String,Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("title", sku.getTitle());
            map.put("price", sku.getPrice());
            map.put("image", StringUtils.substringBefore(sku.getImages(), ","));
            skuMap.add(map);
        });
        // 获取价格
        Set<Long> price = skuList.stream().map(Sku::getPrice).collect(Collectors.toSet());


        // 准备规格参数,规格参数的key在SpecParam中， 规格参数的值在SpuDetail
        Map<String, Object> specs = new HashMap<>();
        // 规格参数key，从SpecParam中获取
        List<SpecParam> specParams = specClient.queryParam(null, spu.getCid3(), true);
        // 规格参数value，去SpuDetail中获取
        SpuDetail spuDetail = goodsClient.queryDetailBySpuId(spuId);
        // 获取通用规格参数值
        Map<Long, Object> genericSpec = JsonUtils.toMap(spuDetail.getGenericSpec(), Long.class, Object.class);
        // 获取特殊规格参数值
        Map<Long, List<String>> specialSpec =
                JsonUtils.nativeRead(spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {
        });

        for (SpecParam param : specParams) {
            // 获取规格参数名称，作为key
            String key = param.getName();
            // 规格参数值
            Object value = null;
            // 判断是否是通用
            if(param.getGeneric()){
                // 通用规格参数
                value = genericSpec.get(param.getId());
                // 判断是否是数值类型
                if(param.getNumeric()){
                    // 判断分段，并且存储
                    value = chooseSegment(value.toString(), param);
                }
            }else{
                // 特有规格参数
                value = specialSpec.get(param.getId());
            }
            value = value == null ? "其他" : value;
            // 存入规格参数map
            specs.put(key, value);
        }

        // 创建一个Goods
        Goods goods = new Goods();
        goods.setAll(all);// 所有搜索字段拼接
        goods.setBrandId(spu.getBrandId());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setId(spuId);
        goods.setPrice(price);// 当前spu下所有sku 的价格几何
        goods.setSkus(JsonUtils.toString(skuMap));// 当前spu下所有sku的json数组
        goods.setSpecs(specs);// 可以用来搜索的规格参数
        goods.setSubTitle(spu.getSubTitle());
        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其他";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public PageResult<Goods> search(SearchRequest request) {
        // 判断用户是否填写条件
        String key = request.getKey();
        if(StringUtils.isBlank(key)){
            throw new LyException(ExceptionEnum.NO_SEARCH_RESULT);
        }

        // 创建查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 0、过滤字段
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "subTitle", "skus"}, null));
        // 1、搜索条件
        QueryBuilder basicQuery = buildBasicQuery(request);
        queryBuilder.withQuery(basicQuery);
        // 2、分页
        int page = request.getPage() - 1;
        int size = request.getSize();
        queryBuilder.withPageable(PageRequest.of(page, size));

        // 3、添加聚合条件
        String categoryAggName = "categoryAgg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        String brandAggName = "brandAgg";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        // 4、搜索
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);

        // 5、解析搜索结果
        List<Goods> list = result.getContent(); // 当前页数据
        long total = result.getTotalElements();// 总条数
        int totalPages = result.getTotalPages();// 总页数

        // 6、准备过滤待选项
        List<Map<String,Object>> filterList = new ArrayList<>();

        // 7、解析聚合结果
        Aggregations aggs = result.getAggregations();
        // 7.1 解析分类聚合
        List<Long> idList = handleCategoryAgg(aggs.get(categoryAggName), filterList);
        // 7.2 解析品牌聚合
        handleBrandAgg(aggs.get(brandAggName), filterList);

        // 5.3、规格参数的聚合
        if(idList != null && idList.size() == 1){
            // 当分类只剩下一个时，我们对规格进行聚合
            handleSpecAgg(idList.get(0), filterList, basicQuery);
        }

        return new SearchResult(total, totalPages, list, filterList);
    }

    private QueryBuilder buildBasicQuery(SearchRequest request) {
        // 组合查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 1、添加查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()));
        // 2、添加过滤条件
        Map<String, String> filters = request.getFilters();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            if(!key.equals("brandId") && !key.equals("cid3")){
                key = "specs." + key + ".keyword";
            }
            String value = entry.getValue();
            boolQueryBuilder.filter(QueryBuilders.termQuery(key, value));
        }
        return boolQueryBuilder;
    }

    private void handleSpecAgg(Long cid, List<Map<String, Object>> filterList, QueryBuilder basicQuery) {
        // 1、查询当前分类下，所有可以搜索的规格参数
        List<SpecParam> specParams = specClient.queryParam(null, cid, true);

        // 2、构建聚合条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 2.1 添加搜索条件
        queryBuilder.withQuery(basicQuery);
        // 2.2 设置搜索结果为1
        queryBuilder.withPageable(PageRequest.of(0, 1));
        // 2.3 添加聚合条件
        for (SpecParam param : specParams) {
            String name = param.getName();
            // 聚合的字段，因为规格是一个对象，对象名是specs,所以属性名是：specs.属性名
            // 但是，因为类型是字符串，所以智能推断数据类型时，会在名称后再拼接一个".keyword"作为不分词字段
            queryBuilder.addAggregation(AggregationBuilders.terms(name).field("specs." + name + ".keyword"));
        }
        // 3、搜索和聚合
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);

        // 4、解析聚合结果
        Aggregations aggs = result.getAggregations();
        for (SpecParam param : specParams) {
            // 获取聚合结果
            StringTerms terms = aggs.get(param.getName());
            // 获取桶
            List<String> options = terms.getBuckets().stream().map(b -> b.getKeyAsString())
                    .filter(s -> StringUtils.isNotBlank(s))
                    .collect(Collectors.toList());
            // 封装结果
            Map<String, Object> map = new HashMap<>();
            map.put("k", param.getName());
            map.put("options", options);
            filterList.add(map);
        }
    }

    private void handleBrandAgg(LongTerms brandAgg, List<Map<String, Object>> filterList) {
        // 解析聚合中的桶
        List<LongTerms.Bucket> buckets = brandAgg.getBuckets();
        // 把桶中的id取出，形成品牌的id集合
        List<Long> idList = buckets.stream().
                map(b -> b.getKeyAsNumber().longValue())
                .collect(Collectors.toList());
        // 根据id查询所有品牌
        List<Brand> brands = brandClient.queryByIdList(idList);

        Map<String, Object> filter = new HashMap<>();
        filter.put("k", "brandId");
        filter.put("options", brands);
        filterList.add(filter);
    }

    private List<Long> handleCategoryAgg(LongTerms categoryAgg, List<Map<String, Object>> filterList) {
        // 解析聚合中的桶
        List<LongTerms.Bucket> buckets = categoryAgg.getBuckets();
        // 把桶中的id取出，形成分类的id集合
        List<Long> idList = buckets.stream().
                map(b -> b.getKeyAsNumber().longValue())
                .collect(Collectors.toList());
        // 根据id查询分类
        List<Category> categories = categoryClient.queryByIdList(idList);
        Map<String, Object> filter = new HashMap<>();
        filter.put("k", "cid3");
        filter.put("options", categories);
        filterList.add(filter);
        return idList;
    }

    public void insertOrUpdate(Long spuId) {
        Spu spu = goodsClient.querySpuById(spuId);
        Goods goods = buildGoods(spu);
        goodsRepository.save(goods);
    }

    public void delete(Long spuId) {
        goodsRepository.deleteById(spuId);
    }
}
