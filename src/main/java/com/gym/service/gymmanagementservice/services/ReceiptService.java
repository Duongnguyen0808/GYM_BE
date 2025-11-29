package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.models.Sale;
import com.gym.service.gymmanagementservice.models.Transaction;
import com.gym.service.gymmanagementservice.repositories.SaleRepository;
import com.gym.service.gymmanagementservice.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final TransactionRepository transactionRepository;
    private final SaleRepository saleRepository;

    public byte[] generateTransactionReceipt(Long transactionId) throws IOException {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch: " + transactionId));

        // Tính số dòng sản phẩm (có thể có nhiều dòng nếu tên dài)
        int itemCount = tx.getSale() != null && tx.getSale().getSaleDetails() != null ? tx.getSale().getSaleDetails().size() : (tx.getMemberPackage() != null ? 1 : 0);
        int estimatedRows = itemCount * 2; // Ước tính mỗi item có thể chiếm 2 dòng nếu tên dài
        
        float width = 400f; // Tăng width để có không gian cho padding
        float height = 280f + estimatedRows * 24f; // Tăng height để có padding và khoảng cách

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(width, height));
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDType0Font font = loadUnicodeFont(doc);
                PDType0Font mono = font;
                if (mono == null) mono = loadSpecificFont(doc, "C:/Windows/Fonts/cour.ttf");

                // Padding 16px = 12pt (1px ≈ 0.75pt)
                float padding = 12f;
                float tablePadding = 6f; // 8px = 6pt
                float tableLeft = padding;
                float tableRight = width - padding;
                float tableWidth = tableRight - tableLeft;

                // === PHẦN THÔNG TIN CHUNG ===
                float headerSize = 18f;
                if (font == null) cs.setFont(PDType1Font.HELVETICA_BOLD, headerSize); else cs.setFont(font, headerSize);
                String brand = text(font, "MOUSE GYM");
                float headerX = (width - textWidth((font != null ? font : PDType1Font.HELVETICA_BOLD), headerSize, brand)) / 2f;
                cs.beginText();
                cs.newLineAtOffset(headerX, height - padding - 20);
                cs.showText(brand);
                cs.endText();

                float bodySize = 11f;
                if (font == null) cs.setFont(PDType1Font.HELVETICA, bodySize); else cs.setFont(font, bodySize);
                float y = height - padding - 50;
                
                y = writeLine(cs, (int)padding, (int)y, text(font, "PHIẾU THU"));
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String memberName = tx.getMemberPackage() != null && tx.getMemberPackage().getMember() != null ? tx.getMemberPackage().getMember().getFullName() : (tx.getSale() != null && tx.getSale().getMember() != null ? tx.getSale().getMember().getFullName() : "-");
                y = writeLine(cs, (int)padding, (int)y, text(font, "Ngày: " + formatDate(tx.getTransactionDate(), fmt)));
                y = writeLine(cs, (int)padding, (int)y, text(font, "Mã GD: " + tx.getId()));
                y = writeLine(cs, (int)padding, (int)y, text(font, "Hình thức: " + vnPayment(tx.getPaymentMethod())));
                y = writeLine(cs, (int)padding, (int)y, text(font, "Loại: " + vnKind(tx.getKind())));
                if (!"-".equals(memberName)) y = writeLine(cs, (int)padding, (int)y, text(font, "Khách: " + memberName));

                // Đường kẻ ngăn cách
                y -= 12;
                cs.setLineWidth(0.5f);
                cs.setStrokingColor(0.88f, 0.88f, 0.88f); // #E0E0E0
                cs.moveTo(tableLeft, y);
                cs.lineTo(tableRight, y);
                cs.stroke();
                cs.setStrokingColor(0f, 0f, 0f); // Reset về đen

                // === BẢNG SẢN PHẨM ===
                y -= 20;
                float rowHeight = 20f;
                float fontSize = 11f;
                if (mono != null) cs.setFont(mono, fontSize); else cs.setFont(PDType1Font.COURIER, fontSize);

                // Header của bảng - tính vị trí các cột
                float col1Right = tableLeft + tableWidth * 0.50f; // Kết thúc cột Tên
                float col2Left = col1Right + 10f; // Bắt đầu cột SL
                float col2Right = tableLeft + tableWidth * 0.65f; // Kết thúc cột SL
                float col3Left = col2Right + 10f; // Bắt đầu cột Đơn giá
                float col3Right = tableLeft + tableWidth * 0.80f; // Kết thúc cột Đơn giá
                float col4Left = col3Right + 10f; // Bắt đầu cột Thành tiền
                float col4Right = tableRight - tablePadding; // Kết thúc cột Thành tiền
                
                float col1Center = tableLeft + tablePadding; // Tên (căn trái)
                float col2Center = (col2Left + col2Right) / 2f; // SL (căn giữa)
                float col3Center = col3Right; // Đơn giá (căn phải)
                float col4Center = col4Right; // Thành tiền (căn phải)

                // Vẽ header với border
                drawTableRowWithBorders(cs, y, tableLeft, tableRight, col1Center, col2Center, col3Center, col4Center,
                        col1Right, col2Left, col2Right, col3Left, col3Right, col4Left,
                        text(mono, "Tên"), text(mono, "SL"), text(mono, "Đơn giá"), text(mono, "Thành tiền"), 
                        mono != null ? mono : PDType1Font.COURIER, fontSize, true);
                y -= rowHeight;

                // Dòng sản phẩm
                if (tx.getMemberPackage() != null) {
                    String name = tx.getMemberPackage().getGymPackage() != null ? tx.getMemberPackage().getGymPackage().getName() : "Gói tập";
                    String qty = "1";
                    String unit = formatMoneyNoCurrency(tx.getAmount());
                    String total = formatMoneyNoCurrency(tx.getAmount());
                    y = drawProductRowWithBorders(cs, y, tableLeft, tableRight, col1Center, col2Center, col3Center, col4Center,
                            col1Right, col2Left, col2Right, col3Left, col3Right, col4Left,
                            name, qty, unit, total, mono != null ? mono : PDType1Font.COURIER, fontSize, col1Right - col1Center - tablePadding);
                } else if (tx.getSale() != null && tx.getSale().getSaleDetails() != null) {
                    for (var d : tx.getSale().getSaleDetails()) {
                        String name = d.getProduct() != null ? d.getProduct().getName() : "Sản phẩm";
                        String qty = String.valueOf(d.getQuantity() != null ? d.getQuantity() : 0);
                        String unit = formatMoneyNoCurrency(d.getPriceAtSale());
                        java.math.BigDecimal totalVal = (d.getPriceAtSale() != null ? d.getPriceAtSale() : java.math.BigDecimal.ZERO)
                                .multiply(new java.math.BigDecimal(d.getQuantity() != null ? d.getQuantity() : 0));
                        String total = formatMoneyNoCurrency(totalVal);
                        y = drawProductRowWithBorders(cs, y, tableLeft, tableRight, col1Center, col2Center, col3Center, col4Center,
                                col1Right, col2Left, col2Right, col3Left, col3Right, col4Left,
                                name, qty, unit, total, mono != null ? mono : PDType1Font.COURIER, fontSize, col1Right - col1Center - tablePadding);
                        y -= 4;
                    }
                }

                // === DÒNG TỔNG TIỀN RIÊNG BIỆT ===
                y -= 16;
                float totalSize = 12f;
                if (font == null) cs.setFont(PDType1Font.HELVETICA_BOLD, totalSize); else cs.setFont(font, totalSize);
                String totalBottom = "TỔNG CỘNG: " + formatMoneyNoCurrency(tx.getAmount()) + "₫";
                float totalBottomX = tableRight - textWidth((font != null ? font : PDType1Font.HELVETICA_BOLD), totalSize, totalBottom);
                cs.beginText();
                cs.newLineAtOffset(totalBottomX, y);
                cs.showText(text(font, totalBottom));
                cs.endText();
            }

            doc.save(baos);
        }
        return baos.toByteArray();
    }

    public byte[] generateSaleReceipt(Long saleId) throws IOException {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn: " + saleId));

        // Tính số dòng sản phẩm (có thể có nhiều dòng nếu tên dài)
        int itemCount = sale.getSaleDetails() != null ? sale.getSaleDetails().size() : 0;
        int estimatedRows = itemCount * 2; // Ước tính mỗi item có thể chiếm 2 dòng nếu tên dài
        
        float width = 400f; // Tăng width để có không gian cho padding
        float height = 280f + estimatedRows * 24f; // Tăng height để có padding và khoảng cách

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(width, height));
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDType0Font font = loadUnicodeFont(doc);
                PDType0Font mono = font;
                if (mono == null) mono = loadSpecificFont(doc, "C:/Windows/Fonts/cour.ttf");

                // Padding 16px = 12pt (1px ≈ 0.75pt)
                float padding = 12f;
                float tablePadding = 6f; // 8px = 6pt
                float tableLeft = padding;
                float tableRight = width - padding;
                float tableWidth = tableRight - tableLeft;

                // === PHẦN THÔNG TIN CHUNG ===
                float headerSize = 18f;
                if (font == null) cs.setFont(PDType1Font.HELVETICA_BOLD, headerSize); else cs.setFont(font, headerSize);
                String brand = text(font, "MOUSE GYM");
                float headerX = (width - textWidth((font != null ? font : PDType1Font.HELVETICA_BOLD), headerSize, brand)) / 2f;
                cs.beginText();
                cs.newLineAtOffset(headerX, height - padding - 20);
                cs.showText(brand);
                cs.endText();

                float bodySize = 11f;
                if (font == null) cs.setFont(PDType1Font.HELVETICA, bodySize); else cs.setFont(font, bodySize);
                float y = height - padding - 50;
                
                y = writeLine(cs, (int)padding, (int)y, text(font, "PHIẾU THANH TOÁN"));
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String staffName = sale.getUser() != null ? sale.getUser().getFullName() : "-";
                y = writeLine(cs, (int)padding, (int)y, text(font, "NV: " + staffName));
                y = writeLine(cs, (int)padding, (int)y, text(font, "Ngày: " + formatDate(sale.getSaleDate(), fmt)));
                y = writeLine(cs, (int)padding, (int)y, text(font, "Mã HĐ: " + sale.getId()));
                if (sale.getMember() != null) y = writeLine(cs, (int)padding, (int)y, text(font, "Khách: " + sale.getMember().getFullName()));

                // Đường kẻ ngăn cách
                y -= 12;
                cs.setLineWidth(0.5f);
                cs.setStrokingColor(0.88f, 0.88f, 0.88f); // #E0E0E0
                cs.moveTo(tableLeft, y);
                cs.lineTo(tableRight, y);
                cs.stroke();
                cs.setStrokingColor(0f, 0f, 0f); // Reset về đen

                // === BẢNG SẢN PHẨM ===
                y -= 20;
                float rowHeight = 20f;
                float fontSize = 11f;
                if (mono != null) cs.setFont(mono, fontSize); else cs.setFont(PDType1Font.COURIER, fontSize);

                // Header của bảng - tính vị trí các cột
                float col1Right = tableLeft + tableWidth * 0.50f; // Kết thúc cột Tên
                float col2Left = col1Right + 10f; // Bắt đầu cột SL
                float col2Right = tableLeft + tableWidth * 0.65f; // Kết thúc cột SL
                float col3Left = col2Right + 10f; // Bắt đầu cột Đơn giá
                float col3Right = tableLeft + tableWidth * 0.80f; // Kết thúc cột Đơn giá
                float col4Left = col3Right + 10f; // Bắt đầu cột Thành tiền
                float col4Right = tableRight - tablePadding; // Kết thúc cột Thành tiền
                
                float col1Center = tableLeft + tablePadding; // Tên (căn trái)
                float col2Center = (col2Left + col2Right) / 2f; // SL (căn giữa)
                float col3Center = col3Right; // Đơn giá (căn phải)
                float col4Center = col4Right; // Thành tiền (căn phải)

                // Vẽ header với border
                drawTableRowWithBorders(cs, y, tableLeft, tableRight, col1Center, col2Center, col3Center, col4Center,
                        col1Right, col2Left, col2Right, col3Left, col3Right, col4Left,
                        text(mono, "Tên"), text(mono, "SL"), text(mono, "Đơn giá"), text(mono, "Thành tiền"), 
                        mono != null ? mono : PDType1Font.COURIER, fontSize, true);
                y -= rowHeight;

                // Dòng sản phẩm
                if (sale.getSaleDetails() != null) {
                    for (var d : sale.getSaleDetails()) {
                        String name = d.getProduct() != null ? d.getProduct().getName() : "Sản phẩm";
                        String qty = String.valueOf(d.getQuantity() != null ? d.getQuantity() : 0);
                        String unit = formatMoneyNoCurrency(d.getPriceAtSale());
                        java.math.BigDecimal totalVal = (d.getPriceAtSale() != null ? d.getPriceAtSale() : java.math.BigDecimal.ZERO)
                                .multiply(new java.math.BigDecimal(d.getQuantity() != null ? d.getQuantity() : 0));
                        String total = formatMoneyNoCurrency(totalVal);
                        y = drawProductRowWithBorders(cs, y, tableLeft, tableRight, col1Center, col2Center, col3Center, col4Center,
                                col1Right, col2Left, col2Right, col3Left, col3Right, col4Left,
                                name, qty, unit, total, mono != null ? mono : PDType1Font.COURIER, fontSize, col1Right - col1Center - tablePadding);
                        y -= 4;
                    }
                }

                // === DÒNG TỔNG TIỀN RIÊNG BIỆT ===
                y -= 16;
                float totalSize = 12f;
                if (font == null) cs.setFont(PDType1Font.HELVETICA_BOLD, totalSize); else cs.setFont(font, totalSize);
                String totalBottom = "TỔNG CỘNG: " + formatMoneyNoCurrency(sale.getTotalAmount()) + "₫";
                float totalBottomX = tableRight - textWidth((font != null ? font : PDType1Font.HELVETICA_BOLD), totalSize, totalBottom);
                cs.beginText();
                cs.newLineAtOffset(totalBottomX, y);
                cs.showText(text(font, totalBottom));
                cs.endText();
            }

            doc.save(baos);
        }
        return baos.toByteArray();
    }

    public byte[] generateErrorReceipt(String title, String message) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.beginText();
                cs.newLineAtOffset(50, 750);
                cs.showText(sanitize(title != null ? title : "BIEN NHAN"));
                cs.endText();

                cs.setFont(PDType1Font.HELVETICA, 12);
                int y = 720;
                y = writeLine(cs, 50, y, sanitize(message != null ? message : "Khong co du lieu"));
            }
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private int writeLine(PDPageContentStream cs, int x, int y, String text) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - 18;
    }

    private String formatMoney(BigDecimal amount) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(amount != null ? amount : java.math.BigDecimal.ZERO) + " VNĐ";
    }

    private String formatMoneyNoCurrency(BigDecimal amount) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(amount != null ? amount : java.math.BigDecimal.ZERO);
    }

    private String sanitize(String input) {
        String n = Normalizer.normalize(input, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        n = n.replace('Đ', 'D').replace('đ', 'd');
        return n;
    }

    private String formatDate(java.time.OffsetDateTime dt, DateTimeFormatter fmt) {
        if (dt == null) return "-";
        return dt.atZoneSameInstant(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(fmt);
    }

    private PDType0Font loadUnicodeFont(PDDocument doc) {
        String[] candidates = {
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/tahoma.ttf",
                "C:/Windows/Fonts/segoeui.ttf"
        };
        for (String path : candidates) {
            try {
                java.io.File f = new java.io.File(path);
                if (f.exists()) {
                    return PDType0Font.load(doc, f);
                }
            } catch (IOException ignored) { }
        }
        return null;
    }

    private PDType0Font loadSpecificFont(PDDocument doc, String path) {
        try {
            java.io.File f = new java.io.File(path);
            if (f.exists()) return PDType0Font.load(doc, f);
        } catch (IOException ignored) {}
        return null;
    }

    private String text(PDType0Font unicode, String s) {
        return unicode != null ? s : sanitize(s);
    }

    private String vnPayment(com.gym.service.gymmanagementservice.models.PaymentMethod pm) {
        if (pm == null) return "-";
        switch (pm) {
            case CASH: return "Tiền mặt";
            case BANK_TRANSFER: return "Chuyển khoản";
            case VN_PAY: return "VNPay";
            default: return pm.name();
        }
    }

    private String vnKind(com.gym.service.gymmanagementservice.models.TransactionKind k) {
        if (k == null) return "-";
        switch (k) {
            case SUBSCRIPTION_NEW: return "Đăng ký gói";
            case SUBSCRIPTION_RENEW: return "Gia hạn gói";
            case SUBSCRIPTION_UPGRADE: return "Nâng cấp gói";
            case SALE: return "Bán hàng";
            case REFUND: return "Hoàn tiền";
            default: return k.name();
        }
    }

    private void writeColumns(PDPageContentStream cs, int y, float width, String c1, String c2, String c3, String c4) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(20, y);
        cs.showText(c1);
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(width - 150, y);
        cs.showText(c2);
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(width - 90, y);
        cs.showText(c3);
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(width - 50, y);
        cs.showText(c4);
        cs.endText();
    }

    private int writeRow(PDPageContentStream cs, int y, float width, String n, String q, String u, String t, org.apache.pdfbox.pdmodel.font.PDFont font, float size) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(20, y);
        cs.showText(n);
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(width - 150, y);
        cs.showText(q);
        cs.endText();
        cs.beginText();
        float ux = width - 90 - textWidth(font, size, u);
        cs.newLineAtOffset(ux, y);
        cs.showText(u);
        cs.endText();
        cs.beginText();
        float tx = width - 20 - textWidth(font, size, t);
        cs.newLineAtOffset(tx, y);
        cs.showText(t);
        cs.endText();
        return y - 18;
    }

    private float textWidth(org.apache.pdfbox.pdmodel.font.PDFont font, float size, String s) throws IOException {
        try {
            return font.getStringWidth(s) / 1000f * size;
        } catch (Exception e) {
            return s.length() * size * 0.6f;
        }
    }

    /**
     * Vẽ một dòng header của bảng với border dọc
     */
    private void drawTableRowWithBorders(PDPageContentStream cs, float y, float tableLeft, float tableRight,
                              float col1Center, float col2Center, float col3Center, float col4Center,
                              float col1Right, float col2Left, float col2Right, float col3Left, float col3Right, float col4Left,
                              String name, String qty, String unit, String total,
                              org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, boolean isHeader) throws IOException {
        float rowHeight = 20f;
        float topY = y;
        float bottomY = y - rowHeight;
        
        // Vẽ border ngang trên
        cs.setLineWidth(0.5f);
        cs.setStrokingColor(0.88f, 0.88f, 0.88f); // #E0E0E0
        cs.moveTo(tableLeft, topY);
        cs.lineTo(tableRight, topY);
        cs.stroke();
        
        // Vẽ border dọc
        cs.moveTo(col1Right, topY);
        cs.lineTo(col1Right, bottomY);
        cs.stroke();
        
        cs.moveTo(col2Left, topY);
        cs.lineTo(col2Left, bottomY);
        cs.stroke();
        
        cs.moveTo(col2Right, topY);
        cs.lineTo(col2Right, bottomY);
        cs.stroke();
        
        cs.moveTo(col3Left, topY);
        cs.lineTo(col3Left, bottomY);
        cs.stroke();
        
        cs.moveTo(col3Right, topY);
        cs.lineTo(col3Right, bottomY);
        cs.stroke();
        
        cs.moveTo(col4Left, topY);
        cs.lineTo(col4Left, bottomY);
        cs.stroke();
        
        // Vẽ border ngang dưới
        cs.moveTo(tableLeft, bottomY);
        cs.lineTo(tableRight, bottomY);
        cs.stroke();
        cs.setStrokingColor(0f, 0f, 0f);

        // Vẽ text
        float textY = y - 2;
        // Tên - căn trái
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(col1Center, textY);
        cs.showText(name);
        cs.endText();

        // SL - căn giữa
        float qtyWidth = textWidth(font, fontSize, qty);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(col2Center - qtyWidth / 2, textY);
        cs.showText(qty);
        cs.endText();

        // Đơn giá - căn phải
        float unitWidth = textWidth(font, fontSize, unit);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(col3Center - unitWidth, textY);
        cs.showText(unit);
        cs.endText();

        // Thành tiền - căn phải
        float totalWidth = textWidth(font, fontSize, total);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(col4Center - totalWidth, textY);
        cs.showText(total);
        cs.endText();
    }

    /**
     * Vẽ một dòng sản phẩm với border dọc và khả năng xuống dòng nếu tên dài
     */
    private float drawProductRowWithBorders(PDPageContentStream cs, float y, float tableLeft, float tableRight,
                                  float col1Center, float col2Center, float col3Center, float col4Center,
                                  float col1Right, float col2Left, float col2Right, float col3Left, float col3Right, float col4Left,
                                  String name, String qty, String unit, String total,
                                  org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, float maxNameWidth) throws IOException {
        float rowHeight = 20f;
        float topY = y;

        // Xử lý tên sản phẩm dài - tự xuống dòng
        java.util.List<String> nameLines = wrapText(name, font, fontSize, maxNameWidth);
        float actualRowHeight = Math.max(rowHeight, nameLines.size() * 14f + 4f);
        float bottomY = topY - actualRowHeight;

        // Vẽ border ngang trên
        cs.setLineWidth(0.5f);
        cs.setStrokingColor(0.88f, 0.88f, 0.88f); // #E0E0E0
        cs.moveTo(tableLeft, topY);
        cs.lineTo(tableRight, topY);
        cs.stroke();
        
        // Vẽ border dọc
        cs.moveTo(col1Right, topY);
        cs.lineTo(col1Right, bottomY);
        cs.stroke();
        
        cs.moveTo(col2Left, topY);
        cs.lineTo(col2Left, bottomY);
        cs.stroke();
        
        cs.moveTo(col2Right, topY);
        cs.lineTo(col2Right, bottomY);
        cs.stroke();
        
        cs.moveTo(col3Left, topY);
        cs.lineTo(col3Left, bottomY);
        cs.stroke();
        
        cs.moveTo(col3Right, topY);
        cs.lineTo(col3Right, bottomY);
        cs.stroke();
        
        cs.moveTo(col4Left, topY);
        cs.lineTo(col4Left, bottomY);
        cs.stroke();
        
        // Vẽ border ngang dưới
        cs.moveTo(tableLeft, bottomY);
        cs.lineTo(tableRight, bottomY);
        cs.stroke();
        cs.setStrokingColor(0f, 0f, 0f);

        float nameStartY = topY - 2;
        
        // Vẽ tên (có thể nhiều dòng)
        cs.beginText();
        cs.setFont(font, fontSize);
        float nameY = nameStartY;
        for (int i = 0; i < nameLines.size(); i++) {
            if (i > 0) {
                cs.endText();
                cs.beginText();
                nameY -= 14; // Khoảng cách dòng
            }
            cs.newLineAtOffset(col1Center, nameY);
            cs.showText(nameLines.get(i));
        }
        cs.endText();

        // Vẽ SL - căn giữa (ở dòng đầu tiên)
        float qtyWidth = textWidth(font, fontSize, qty);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(col2Center - qtyWidth / 2, nameStartY);
        cs.showText(qty);
        cs.endText();

        // Vẽ Đơn giá - căn phải (ở dòng đầu tiên)
        float unitWidth = textWidth(font, fontSize, unit);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(col3Center - unitWidth, nameStartY);
        cs.showText(unit);
        cs.endText();

        // Vẽ Thành tiền - căn phải (ở dòng đầu tiên)
        float totalWidth = textWidth(font, fontSize, total);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(col4Center - totalWidth, nameStartY);
        cs.showText(total);
        cs.endText();

        return bottomY;
    }

    /**
     * Wrap text thành nhiều dòng nếu quá dài
     */
    private java.util.List<String> wrapText(String text, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, float maxWidth) throws IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine.toString() + " " + word : word;
            float testWidth = textWidth(font, fontSize, testLine);

            if (testWidth <= maxWidth) {
                currentLine.append(currentLine.length() > 0 ? " " : "").append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // Từ đơn quá dài, cắt nó
                    lines.add(word);
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        if (lines.isEmpty()) {
            lines.add("");
        }

        return lines;
    }
}
