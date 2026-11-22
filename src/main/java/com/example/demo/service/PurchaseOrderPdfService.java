package com.example.demo.service;

import com.example.demo.model.PurchaseAgreement;
import com.example.demo.model.User;
import com.example.demo.model.Vehicle;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PurchaseOrderPdfService {

    @SuppressWarnings("deprecation")
    public byte[] generarOrdenCompraPdf(
            PurchaseAgreement agreement,
            Vehicle vehicle,
            User buyer,
            User seller
    ) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 42, 42, 42, 42);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(15, 23, 42));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139));
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(15, 23, 42));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(30, 41, 59));
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, new Color(71, 85, 105));
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(15, 23, 42));
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(10, 118, 224));
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.5f, new Color(120, 120, 130));

            Paragraph brand = new Paragraph("Remaneja", brandFont);
            brand.setSpacingAfter(2f);
            document.add(brand);

            Paragraph sub = new Paragraph("Plataforma de Compra y Venta de Vehículos Usados — Orden de Compra Oficial", subtitleFont);
            sub.setSpacingAfter(14f);
            document.add(sub);

            Paragraph docTitle = new Paragraph("ORDEN DE COMPRA #" + agreement.getId(), titleFont);
            docTitle.setSpacingAfter(12f);
            document.add(docTitle);

            Locale localeCo = new Locale("es", "CO");
            NumberFormat currencyFmt = NumberFormat.getCurrencyInstance(localeCo);
            currencyFmt.setMaximumFractionDigits(0);
            NumberFormat intFmt = NumberFormat.getIntegerInstance(localeCo);

            String fechaEmision = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String fechaAcuerdo = agreement.getAgreementDate() != null
                    ? agreement.getAgreementDate().toString()
                    : fechaEmision;

            document.add(new Paragraph("1. Datos de la Operación", sectionFont));
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(6f);
            infoTable.setSpacingAfter(14f);
            infoTable.setWidths(new float[]{38f, 62f});

            agregarFila(infoTable, "Número de Orden / Acuerdo", "#" + agreement.getId(), labelFont, valueFont);
            agregarFila(infoTable, "Estado del Acuerdo", textoSeguro(agreement.getAgreementStatus()), labelFont, valueFont);
            agregarFila(infoTable, "Fecha del Acuerdo", fechaAcuerdo, labelFont, valueFont);
            agregarFila(infoTable, "Fecha de Emisión PDF", fechaEmision, labelFont, valueFont);
            document.add(infoTable);

            document.add(new Paragraph("2. Partes Involucradas", sectionFont));
            PdfPTable partiesTable = new PdfPTable(2);
            partiesTable.setWidthPercentage(100);
            partiesTable.setSpacingBefore(6f);
            partiesTable.setSpacingAfter(14f);
            partiesTable.setWidths(new float[]{50f, 50f});

            partiesTable.addCell(celdaBloque(
                    "COMPRADOR",
                    "Nombre: " + (buyer != null ? textoSeguro(buyer.getName()) : "ID #" + agreement.getBuyerId()) + "\n" +
                    "Identificación: " + agreement.getBuyerId() + "\n" +
                    "Correo: " + (buyer != null ? textoSeguro(buyer.getEmail()) : "No registrado") + "\n" +
                    "Teléfono: " + (buyer != null ? textoSeguro(buyer.getPhoneNumber()) : "No registrado"),
                    labelFont, valueFont
            ));

            partiesTable.addCell(celdaBloque(
                    "VENDEDOR",
                    "Nombre: " + (seller != null ? textoSeguro(seller.getName()) : "ID #" + agreement.getSellerId()) + "\n" +
                    "Identificación: " + agreement.getSellerId() + "\n" +
                    "Correo: " + (seller != null ? textoSeguro(seller.getEmail()) : "No registrado") + "\n" +
                    "Teléfono: " + (seller != null ? textoSeguro(seller.getPhoneNumber()) : "No registrado"),
                    labelFont, valueFont
            ));
            document.add(partiesTable);

            document.add(new Paragraph("3. Detalle del Vehículo y Valor Confirmado", sectionFont));
            PdfPTable vehicleTable = new PdfPTable(2);
            vehicleTable.setWidthPercentage(100);
            vehicleTable.setSpacingBefore(6f);
            vehicleTable.setSpacingAfter(16f);
            vehicleTable.setWidths(new float[]{38f, 62f});

            agregarFila(vehicleTable, "Código del Vehículo", "#" + agreement.getVehicleId(), labelFont, valueFont);
            agregarFila(vehicleTable, "Modelo", vehicle != null ? textoSeguro(vehicle.getModel()) : "Vehículo #" + agreement.getVehicleId(), labelFont, valueFont);
            agregarFila(vehicleTable, "Placa", vehicle != null ? textoSeguro(vehicle.getPlate()) : "N/A", labelFont, valueFont);
            agregarFila(vehicleTable, "Tipo / Color",
                    (vehicle != null ? textoSeguro(vehicle.getType()) : "N/A") + " / " +
                    (vehicle != null ? textoSeguro(vehicle.getColor()) : "N/A"),
                    labelFont, valueFont);
            agregarFila(vehicleTable, "Año / Kilometraje",
                    (vehicle != null ? vehicle.getReleaseYear() : "N/A") + " — " +
                    (vehicle != null && vehicle.getKms() != null ? intFmt.format(vehicle.getKms()) + " km" : "0 km"),
                    labelFont, valueFont);

            String montoTexto = agreement.getAgreedAmount() != null
                    ? currencyFmt.format(agreement.getAgreedAmount().doubleValue())
                    : "$0";

            PdfPCell lblMonto = new PdfPCell(new Phrase("Monto Acordado Confirmado", labelFont));
            lblMonto.setBackgroundColor(new Color(239, 246, 255));
            lblMonto.setPadding(9f);
            lblMonto.setBorderColor(new Color(191, 219, 254));

            PdfPCell valMonto = new PdfPCell(new Phrase(montoTexto, totalFont));
            valMonto.setBackgroundColor(new Color(239, 246, 255));
            valMonto.setPadding(9f);
            valMonto.setBorderColor(new Color(191, 219, 254));
            valMonto.setHorizontalAlignment(Element.ALIGN_RIGHT);

            vehicleTable.addCell(lblMonto);
            vehicleTable.addCell(valMonto);
            document.add(vehicleTable);

            Paragraph footer = new Paragraph(
                    "Documento generado automáticamente por Remaneja como constancia de confirmación de oferta.",
                    footerFont
            );
            footer.setSpacingBefore(8f);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error generando la orden de compra PDF de Remaneja", e);
        }
    }

    private void agregarFila(PdfPTable table, String campo, String valor, Font labelFont, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(campo, labelFont));
        c1.setBackgroundColor(new Color(248, 250, 252));
        c1.setPadding(7f);
        c1.setBorderColor(new Color(226, 232, 240));

        PdfPCell c2 = new PdfPCell(new Phrase(valor, valueFont));
        c2.setPadding(7f);
        c2.setBorderColor(new Color(226, 232, 240));

        table.addCell(c1);
        table.addCell(c2);
    }

    private PdfPCell celdaBloque(String encabezado, String contenido, Font headerFont, Font bodyFont) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(encabezado + "\n\n", headerFont));
        p.add(new Phrase(contenido, bodyFont));

        PdfPCell cell = new PdfPCell(p);
        cell.setPadding(9f);
        cell.setBackgroundColor(new Color(250, 250, 252));
        cell.setBorderColor(new Color(226, 232, 240));
        return cell;
    }

    private String textoSeguro(String valor) {
        return (valor == null || valor.isBlank()) ? "No especificado" : valor.trim();
    }
}