package com.example.demo.controller;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    // Helper class for the JSON payload
    public static class ReceiptRequest {
        public String companyName = "TECH GEAR LTD";
        public String address = "123 Business Loop, Manila, PH";
        public String birNumber = "BIR-REG: 123-456-789-000";
        public List<Item> items;

        public static class Item {
            public String description;
            public String tagCode; 
            public int quantity;
            public double price;
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateReceipt(@RequestBody ReceiptRequest request) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        
        // Use A4 but we will restrict content to the left
        Document document = new Document(pdf, PageSize.A4);
        // Margin: Top 20, Right 300 (pushes content left), Bottom 20, Left 20
        document.setMargins(20, 300, 20, 20); 

        // Header Section
        document.add(new Paragraph(request.companyName).setBold().setFontSize(12));
        document.add(new Paragraph(request.address).setFontSize(7));
        document.add(new Paragraph(request.birNumber).setFontSize(7).setItalic());
        document.add(new Paragraph("-----------------------------------"));

        // Table: Item(80pt), Tag(60pt), Qty(30pt), Price(50pt)
        float[] columnWidths = {80f, 60f, 30f, 50f};
        Table table = new Table(UnitValue.createPointArray(columnWidths));

        table.addHeaderCell(new Paragraph("Item").setFontSize(8).setBold());
        table.addHeaderCell(new Paragraph("Tag").setFontSize(8).setBold());
        table.addHeaderCell(new Paragraph("Qty").setFontSize(8).setBold());
        table.addHeaderCell(new Paragraph("Price").setFontSize(8).setBold());

        double grandTotal = 0;

        for (ReceiptRequest.Item item : request.items) {
            // Truncate logic
            String desc = (item.description.length() > 15) ? item.description.substring(0, 12) + "..." : item.description;
            String tag = (item.tagCode.length() > 8) ? item.tagCode.substring(0, 8) : item.tagCode;

            table.addCell(new Paragraph(desc).setFontSize(8));
            table.addCell(new Paragraph(tag).setFontSize(8));
            table.addCell(new Paragraph(String.valueOf(item.quantity)).setFontSize(8));
            table.addCell(new Paragraph(String.format("%.2f", item.price)).setFontSize(8));
            
            grandTotal += (item.price * item.quantity);
        }

        document.add(table);
        document.add(new Paragraph("-----------------------------------"));
        document.add(new Paragraph("TOTAL: " + String.format("%.2f", grandTotal)).setBold());
        
        document.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }
}
