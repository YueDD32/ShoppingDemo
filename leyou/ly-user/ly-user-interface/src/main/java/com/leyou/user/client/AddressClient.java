package com.leyou.user.client;

import com.leyou.user.dto.AddressDTO;

import java.util.HashMap;
import java.util.Map;

public class AddressClient {

    public static final Map<Long, AddressDTO> addressList = new HashMap<Long, AddressDTO>(){
        {
            AddressDTO address = new AddressDTO();
            address.setId(1L);
            address.setAddress("航头镇航头路18号传智播客 3号楼");
            address.setCity("上海");
            address.setDistrict("浦东新区");
            address.setName("虎哥");
            address.setPhone("15800000000");
            address.setState("上海");
            address.setZipCode("210000");
            address.setIsDefault(true);
            put(1L, address);

            AddressDTO address2 = new AddressDTO();
            address2.setId(2L);
            address2.setAddress("天堂路 3号楼");
            address2.setCity("北京");
            address2.setDistrict("朝阳区");
            address2.setName("张三");
            address2.setPhone("13600000000");
            address2.setState("北京");
            address2.setZipCode("100000");
            address2.setIsDefault(false);
            put(2L, address2);
        }
    };

    public static AddressDTO findById(Long id){
        return addressList.get(id);
    }
}
