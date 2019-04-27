package com.example.alipaydemo.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.example.alipaydemo.bean.AlipayVo;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 支付宝通用接口
 * @author Mr.Deng
 * @date 2019/4/20 14:17
 * <p>Copyright: Copyright (c) 2018</p>
 * <p>Company: mitesofor </p>
 */
@Slf4j
@RestController
@RequestMapping("/aliPay")
public class AliPayController {
    /**
     * 应用ID,您的APPID，收款账号既是您的APPID对应支付宝账号
     */
    @Value("${aliPay.appID}")
    private String appID;
    /**
     * 商户私钥，您的PKCS8格式RSA2私钥
     */
    @Value("${aliPay.merchantPrivateKey}")
    private String merchantPrivateKey;
    /**
     * 支付宝公钥
     */
    @Value("${aliPay.aliPayPublicKey}")
    private String aliPayPublicKey;
    /**
     * 签名方式
     */
    @Value("${aliPay.signType}")
    private String signType;
    /**
     * 网关
     */
    @Value("${aliPay.gatewayUrl}")
    private String gatewayUrl;
    /**
     * 编码
     */
    @Value("${aliPay.charset}")
    private String charset;
    /**
     * 异步通知地址
     */
    @Value("${aliPay.notifyUrl}")
    private String notifyUrl;
    /**
     * 返回成功地址
     */
    @Value("${aliPay.returnUrl}")
    private String returnUrl;

    @GetMapping("pay")
    private String alipayPay(AlipayVo vo) throws AlipayApiException {
        //这个应该是从前端端传过来的，这里为了测试就从后台写死了
        vo.setOut_trade_no(UUID.randomUUID().toString().replace("-", ""));
        vo.setTotal_amount("0.01");
        vo.setSubject("支付测试");
        //这个是固定的
        vo.setProduct_code("FAST_INSTANT_TRADE_PAY");
        String json = new Gson().toJson(vo);
        System.out.println(json);

        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl, appID, merchantPrivateKey, "json",
                charset, aliPayPublicKey, signType);
        // 设置请求参数
        AlipayTradePagePayRequest aliPayRequest = new AlipayTradePagePayRequest();
        aliPayRequest.setReturnUrl(returnUrl);
        aliPayRequest.setNotifyUrl(notifyUrl);
        aliPayRequest.setBizContent(json);
        String result = alipayClient.pageExecute(aliPayRequest).getBody();
        log.info(result);
        //这里生成一个表单，会自动提交
        return result;
    }

    /**
     * @param request
     * @param out_trade_no 商户订单号
     * @param trade_no     支付宝交易凭证号
     * @param trade_status 交易状态
     * @return String
     * @throws
     * @Title: alipayNotify
     * @Description: 支付宝回调接口
     * @author nelson
     */
    @PostMapping("notify")
    private String alipayNotify(HttpServletRequest request, String out_trade_no, String trade_no, String trade_status) {
        log.info("方法名：alipayNotify-----out_trade_no=>" + out_trade_no + " ----trade_no=>" +
                trade_no + "---trade_status=>" + trade_status);
        System.out.println("方法名：alipayNotify-----out_trade_no=>" + out_trade_no + " ----trade_no=>" +
                trade_no + "---trade_status=>" + trade_status);
        Map<String, String[]> requestParams = request.getParameterMap();
        Map<String, String> map = ss(requestParams);
        boolean signVerified;
        try {
            signVerified = AlipaySignature.rsaCheckV1(map, aliPayPublicKey, charset, signType);
        } catch (AlipayApiException e) {
            e.printStackTrace();
            // 验签发生异常,则直接返回失败
            return ("fail");
        }
        if (signVerified) {
            //处理你的业务逻辑，更细订单状态等
            return ("success");
        } else {
            System.out.println("验证失败,不去更新状态");
            return ("fail");
        }
    }

    /**
     * @param request
     * @param out_trade_no 商户订单号
     * @param trade_no     支付宝交易凭证号
     * @param total_amount 交易状态
     * @return String
     * @throws
     * @Title: aliPayReturn
     * @Description: 支付宝回调接口
     * @author nelson
     */
    @GetMapping("return")
    private String aliPayReturn(Map<String, String> params, HttpServletRequest request, String out_trade_no,
                                String trade_no, String total_amount) {
        log.info("方法名：aliPayReturn-----------params=>" + params + "----out_trade_no=>" + out_trade_no + "----trade_no=》" +
                trade_no + "---total_amount=>" + total_amount);
        System.out.println("方法名：aliPayReturn-----------params=>" + params + "----out_trade_no=>" + out_trade_no + "----trade_no=》" +
                trade_no + "---total_amount=>" + total_amount);
        Map<String, String[]> requestParams = request.getParameterMap();
        Map<String, String> map = ss(requestParams);
        boolean signVerified;
        try {
            signVerified = AlipaySignature.rsaCheckV1(map, aliPayPublicKey, charset, signType);
        } catch (AlipayApiException e) {
            e.printStackTrace();
            // 验签发生异常,则直接返回失败
            return ("fail");
        }
        if (signVerified) {
            return ("success");
        } else {
            System.out.println("验证失败,不去更新状态");
            return ("fail");
        }
    }

    private Map<String, String> ss(Map<String, String[]> requestParams) {
        Map<String, String> map = new HashMap<String, String>();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = iter.next();
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
                System.out.println(valueStr);
            }
            map.put(name, valueStr);
        }
        return map;
    }
}
