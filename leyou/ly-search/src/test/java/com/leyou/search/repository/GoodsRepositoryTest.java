package com.leyou.search.repository;

import com.leyou.common.vo.PageResult;
import com.leyou.item.client.GoodsClient;
import com.leyou.item.pojo.Spu;
import com.leyou.search.pojo.Goods;
import com.leyou.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GoodsRepositoryTest {

    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private GoodsRepository repository;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SearchService searchService;

    @Test
    public void createIndex() {
        template.createIndex(Goods.class);
    }

    /*
    循环查询Spu，然后把spu变为goods，写入索引库
     */
    @Test
    public void loadData() {
        int page = 1, rows = 100, size = 0;
        do {
            try {
                // 分页查询spu
                PageResult<Spu> result = goodsClient.querySpuByPage(page, rows, true, null);
                List<Spu> spuList = result.getItems();
                // 准备Goods集合
                List<Goods> goodsList = new ArrayList<>();
                // 把spu集合转为Goods集合
                for (Spu spu : spuList) {
                    Goods goods = null;
                    try {
                        goods = searchService.buildGoods(spu);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    goodsList.add(goods);
                }
                // 写入索引库
                repository.saveAll(goodsList);
                page++;
                size = spuList.size();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        } while (size == 100);
    }
}