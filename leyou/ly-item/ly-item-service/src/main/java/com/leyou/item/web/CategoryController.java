package com.leyou.item.web;

import com.leyou.item.pojo.Category;
import com.leyou.item.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 根据父类目id查询商品分类
     * @param pid
     * @return
     */
    @GetMapping("list")
    public ResponseEntity<List<Category>> queryByParentId(@RequestParam("pid") Long pid){
        List<Category> list = categoryService.queryByParentId(pid);
        return ResponseEntity.ok(list);
    }

    /**
     * 根据多个id查询多个分类
     * @param idList
     * @return
     */
    @GetMapping("list/ids")
    public ResponseEntity<List<Category>> queryByIdList(@RequestParam("ids") List<Long> idList){
        return ResponseEntity.ok(categoryService.queryByIds(idList));
    }
}
