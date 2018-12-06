package com.leyou.page.service;

import com.leyou.item.client.BrandClient;
import com.leyou.item.client.CategoryClient;
import com.leyou.item.client.GoodsClient;
import com.leyou.item.client.SpecClient;
import com.leyou.item.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PageService {

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecClient specClient;

    @Autowired
    private TemplateEngine templateEngine;

    public Map<String, Object> loadItemModel(Long spuId) {

        Map<String, Object> map = new HashMap<>();
        // spu
        Spu spu = goodsClient.querySpuById(spuId);
        map.put("title", spu.getTitle());
        map.put("subTitle", spu.getSubTitle());

        // sku
        List<Sku> skus = spu.getSkus();
        map.put("skus", skus);

        // detail
        SpuDetail spuDetail = spu.getSpuDetail();
        map.put("detail", spuDetail);

        // category
        List<Category> categories = categoryClient.queryByIdList(
                Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        map.put("categories", categories);

        // brand
        Brand brand = brandClient.queryById(spu.getBrandId());
        map.put("brand", brand);

        // specs
        List<SpecGroup> specGroups = specClient.querySpecs(spu.getCid3());
        map.put("specs", specGroups);

        return map;
    }

    public void createItemHtml(Long spuId) {
        // 提供上下文数据
        Context context = new Context();
        context.setVariables(loadItemModel(spuId));

        // 文件目的地
        File file = getFilePath(spuId);

        // 输出流
        try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
            templateEngine.process("item", context, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File getFilePath(Long spuId) {
        File dir = new File("C:\\lesson\\heima46\\nginx-1.12.2\\html\\item");
        if(!dir.exists()){
            dir.mkdirs();
        }
        return new File(dir, spuId + ".html");
    }

    public void deleteItemHtml(Long spuId) {
        File file = getFilePath(spuId);
        if(file.exists()) {
            file.delete();
        }
    }
}
