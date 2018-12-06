package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper groupMapper;

    @Autowired
    private SpecParamMapper paramMapper;

    public List<SpecGroup> queryGroupByCid(Long cid) {
        SpecGroup t = new SpecGroup();
        t.setCid(cid);
        List<SpecGroup> list = groupMapper.select(t);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
        return list;
    }

    public List<SpecParam> queryParams(Long gid, Long cid, Boolean searching) {
        SpecParam t = new SpecParam();
        t.setGroupId(gid);
        t.setCid(cid);
        t.setSearching(searching);
        List<SpecParam> list = paramMapper.select(t);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
        return list;
    }

    public List<SpecGroup> querySpecs(Long cid) {
        // 查询规格组
        List<SpecGroup> specGroups = queryGroupByCid(cid);
        // 查询param
        List<SpecParam> specParams = queryParams(null, cid, null);

        // 对规格进行分组，得到的map的key是groupId，值是该组下所有规格
        Map<Long, List<SpecParam>> map = specParams.stream()
                .collect(Collectors.groupingBy(SpecParam::getGroupId));

        for (SpecGroup group : specGroups) {
            group.setParams(map.get(group.getId()));
        }
        return specGroups;
    }
}
