package com.leyou.item.web;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import com.leyou.item.service.SpecificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("spec")
public class SpecificationController {

    @Autowired
    private SpecificationService specService;

    /**
     * 根据分类查询分类下的所有规格组
     * @param cid
     * @return
     */
    @GetMapping("/groups/{cid}")
    public ResponseEntity<List<SpecGroup>> queryGroupByCid(@PathVariable("cid")Long cid){
        return ResponseEntity.ok(specService.queryGroupByCid(cid));
    }

    /**
     * 根据组id查询组内的所有规格参数
     * @param gid 组id
     * @param cid 分类id
     * @param searching 是否可搜索
     * @return
     */
    @GetMapping("params")
    public ResponseEntity<List<SpecParam>> queryParam(
            @RequestParam(value = "gid", required = false) Long gid,
            @RequestParam(value = "cid", required = false) Long cid,
            @RequestParam(value = "searching", required = false) Boolean searching
    ){
        return ResponseEntity.ok(specService.queryParams(gid, cid, searching));
    }

    /**
     * 查询分类下的规格组及组内参数
     * @param cid
     * @return
     */
    @GetMapping("/{cid}")
    public ResponseEntity<List<SpecGroup>> querySpecs(@PathVariable("cid") Long cid){
        return ResponseEntity.ok(specService.querySpecs(cid));
    }
}
