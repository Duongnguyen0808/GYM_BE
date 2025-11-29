package com.gym.service.gymmanagementservice.utils;

import com.gym.service.gymmanagementservice.config.VNPayConfig; // Import config
import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class VNPayUtil {

    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMACSHA512", e);
        }
    }

    public static String createPaymentUrl(HttpServletRequest req, VNPayConfig config, Long transactionId, BigDecimal amount) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_OrderInfo = "Thanh toan don hang:" + transactionId;
        String orderType = "other";
        String vnp_TxnRef = transactionId.toString();
        String vnp_IpAddr = getIpAddress(req);
        String vnp_TmnCode = config.getTmnCode();

        // Chuyển số tiền sang đơn vị của VNPay (nhân 100)
        long amountLong = amount.multiply(new BigDecimal(100)).longValue();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountLong));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", orderType);

        String locate = req.getParameter("language");
        if (locate == null || locate.isEmpty()) {
            locate = "vn";
        }
        vnp_Params.put("vnp_Locale", locate);
        vnp_Params.put("vnp_ReturnUrl", config.getReturnUrl()); // Sử dụng returnUrl từ config
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(config.getHashSecret(), hashData.toString()); // Sử dụng hashSecret từ config
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return config.getVnpUrl() + "?" + queryUrl; // Sử dụng vnpUrl từ config
    }

    public static String getIpAddress(HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null || ipAdress.isEmpty() || "unknown".equalsIgnoreCase(ipAdress)) {
                ipAdress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAdress == null || ipAdress.isEmpty() || "unknown".equalsIgnoreCase(ipAdress)) {
                ipAdress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAdress == null || ipAdress.isEmpty() || "unknown".equalsIgnoreCase(ipAdress)) {
                ipAdress = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ipAdress == null || ipAdress.isEmpty() || "unknown".equalsIgnoreCase(ipAdress)) {
                ipAdress = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ipAdress == null || ipAdress.isEmpty() || "unknown".equalsIgnoreCase(ipAdress)) {
                ipAdress = request.getRemoteAddr();
            }
            // Handle multiple IPs in X-FORWARDED-FOR header (take the first one)
            if (ipAdress != null && ipAdress.contains(",")) {
                ipAdress = ipAdress.split(",")[0].trim();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }
        return ipAdress;
    }

    // Hàm xác thực chữ ký IPN (Giữ nguyên như trước)
    public static boolean verifyIPNResponse(Map<String, String[]> params, String hashSecret) {
        if (!params.containsKey("vnp_SecureHash")) {
            return false;
        }
        String secureHash = params.get("vnp_SecureHash")[0];

        TreeMap<String, String> sortedParams = new TreeMap<>();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue()[0];
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                if (!fieldName.equals("vnp_SecureHash") && !fieldName.equals("vnp_SecureHashType")) {
                    sortedParams.put(fieldName, fieldValue);
                }
            }
        }

        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            if (hashData.length() > 0) {
                hashData.append('&');
            }
            hashData.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
            hashData.append('=');
            hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
        }

        String calculatedHash = hmacSHA512(hashSecret, hashData.toString());
        return calculatedHash.equalsIgnoreCase(secureHash);
    }
}