package com.leyou.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExceptionEnum {
    // 枚举项必须定义在枚举的最前面
    PRICE_CANNOT_BE_NULL(400, "价格不能为空"),
    SPU_ID_CANNOT_BE_NULL(400, "商品ID为空"),
    CATEGORY_NOT_FOUND(404, "商品分类不存在"),
    BRAND_NOT_FOUND(404, "品牌不存在"),
    SPEC_GROUP_NOT_FOUND(404, "规格组不存在"),
    SPEC_PARAM_NOT_FOUND(404, "规格参数不存在"),
    GOODS_NOT_FOUND(404, "商品不存在"),
    INSERT_BRAND_ERROR(500, "新增品牌失败"),
    INSERT_GOODS_ERROR(500, "新增商品失败"),
    UPDATE_GOODS_ERROR(500, "修改商品失败"),
    FILE_UPLOAD_ERROR(500, "文件上传失败"),
    CREATE_ORDER_ERROR(500, "创建订单失败"),
    INVALID_FILE_TYPE(400, "无效的文件类型"),
    NO_SEARCH_RESULT(404, "没有搜索到结果"),
    CART_NOT_FOUND(404, "购物车中没有商品"),
    INVALID_PARAM_TYPE(400, "无效的参数类型"),
    INVALID_PHONE_CODE(400, "无效的手机验证码"),
    INVALID_UN_PW_ERROR(400, "无效的用户名或密码"),
    STOCK_NOT_ENOUGH(400, "库存不足"),
    UNAUTHORIZED(401, "没有权限访问"),
    CUSTOM_ERROR(0, "未知错误");
    // 类中的其它属性：
    private int status;
    private String message;

    public ExceptionEnum init(int status, String message){
        this.status = status;
        this.message = message;
        return this;
    }
}
