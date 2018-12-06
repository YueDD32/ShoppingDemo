package com.leyou.page.web;

import com.leyou.page.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Controller
public class PageController {

    @Autowired
    private PageService pageService;
    /**
     * 跳转到商品详情页
     * @param id
     * @return
     */
    @GetMapping("item/{id}.html")
    public String toItemPage(@PathVariable("id") Long id, Model model){
        // 加载模型数据
        Map<String, Object> modelData = pageService.loadItemModel(id);
        // 填写模型数据
        model.addAllAttributes(modelData);

        return "item";
    }
}
