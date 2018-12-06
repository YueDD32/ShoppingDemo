package com.leyou.order.service;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.common.dto.CartDTO;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.IdWorker;
import com.leyou.item.client.GoodsClient;
import com.leyou.item.pojo.Sku;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.interceptors.UserInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.user.client.AddressClient;
import com.leyou.user.dto.AddressDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper detailMapper;

    @Autowired
    private OrderStatusMapper statusMapper;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private IdWorker idWorker;

    @Transactional
    public Long createOrder(OrderDTO orderDTO) {
        // 1 准备订单信息
        Order order = new Order();
        // 1.1 生成订单编号
        long orderId = idWorker.nextId();
        order.setOrderId(orderId);

        // 1.2 下单用户
        UserInfo user = UserInterceptor.getUser();
        order.setUserId(user.getId());
        order.setBuyerRate(false);
        order.setBuyerNick(user.getUsername());

        // 1.3 收货人信息
        Long addressId = orderDTO.getAddressId();
        AddressDTO addr = AddressClient.findById(addressId);
        order.setReceiver(addr.getName());
        order.setReceiverAddress(addr.getAddress());
        order.setReceiverCity(addr.getCity());
        order.setReceiverDistrict(addr.getDistrict());
        order.setReceiverMobile(addr.getPhone());
        order.setReceiverState(addr.getState());
        order.setReceiverZip(addr.getZipCode());

        // 1.4 金额相关信息
        List<CartDTO> cartDTOS = orderDTO.getCarts();
        List<Long> idList = cartDTOS.stream()
                .map(CartDTO::getSkuId).collect(Collectors.toList());
        // map的key是skuId，值是购买数量
        Map<Long, Integer> numMap = cartDTOS.stream()
                .collect(Collectors.toMap(CartDTO::getSkuId, CartDTO::getNum));
        // 查询sku
        List<Sku> skuList = goodsClient.querySkuListByIds(idList);

        // 定义OrderDetail集合
        List<OrderDetail> details = new ArrayList<>();

        // 定义总金额
        long total = 0;
        for (Sku sku : skuList) {
            int num = numMap.get(sku.getId());
            total += sku.getPrice() * num;

            // 封装OrderDetail
            OrderDetail detail = new OrderDetail();
            BeanUtils.copyProperties(sku, detail);
            detail.setImage(StringUtils.substringBefore(sku.getImages(), ","));
            detail.setNum(num);
            detail.setOrderId(orderId);
            detail.setSkuId(sku.getId());
            details.add(detail);
        }
        order.setTotalPay(total);
        order.setPostFee(0L);
        order.setActualPay(total + order.getPostFee()/*TODO 减去优惠金额*/);
        order.setPaymentType(orderDTO.getPaymentType());

        // 2 新增订单
        order.setCreateTime(new Date());
        int count = orderMapper.insert(order);
        if (count != 1) {
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }

        // 3 新增订单详情
        count = detailMapper.insertList(details);
        if (count != details.size()) {
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }

        // 4 新增订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(orderId);
        orderStatus.setCreateTime(order.getCreateTime());
        orderStatus.setStatus(OrderStatusEnum.INIT.value());
        count = statusMapper.insertSelective(orderStatus);
        if (count != 1) {
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }

        // 5 减库存
        goodsClient.decreaseStock(cartDTOS);

        return orderId;
    }
}
