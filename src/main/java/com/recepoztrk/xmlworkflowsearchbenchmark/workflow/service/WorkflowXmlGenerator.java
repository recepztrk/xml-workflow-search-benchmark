package com.recepoztrk.xmlworkflowsearchbenchmark.workflow.service;

import org.springframework.stereotype.Component;

/**
 * Benchmark için sentetik ama problem bağlamına uygun XML workflow üretir.
 *
 * Bu sınıfın amacı rastgele oyuncak veri üretmek değildir.
 * Müşteri temsilcisi platformundaki ekran, adım, koşul, alan ve aksiyon yapılarını
 * temsil eden büyük XML dokümanları üretir.
 */
@Component
public class WorkflowXmlGenerator {

    public String generateWorkflowXml(
            String workflowCode,
            String workflowName,
            String domain,
            int version,
            int screenCount
    ) {
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<workflow id=\"").append(workflowCode).append("\" version=\"").append(version).append("\">\n");

        appendMetadata(xml, workflowName, domain);
        appendScreens(xml, screenCount);
        appendTransitions(xml, screenCount);
        appendBusinessRules(xml, screenCount);

        xml.append("</workflow>");

        return xml.toString();
    }

    public void appendMetadata(StringBuilder xml, String workflowName, String domain) {
        xml.append("  <metadata>\n");
        xml.append("    <name>").append(workflowName).append("</name>\n");
        xml.append("    <domain>").append(domain).append("</domain>\n");
        xml.append("    <status>ACTIVE</status>\n");
        xml.append("    <description>")
                .append(workflowName)
                .append(" müşteri temsilcisi ekran akışını, işlem adımlarını ve yönlendirme kurallarını tanımlar.")
                .append("</description>\n");
        xml.append("  </metadata>\n");
    }

    private void appendScreens(StringBuilder xml, int screenCount) {
        xml.append("  <screens>\n");

        for (int i = 1; i <= screenCount; i++) {
            xml.append("    <screen id=\"SCR_").append(i).append("\" type=\"form\">\n");
            xml.append("      <title>").append(generateScreenTitle(i)).append("</title>\n");
            xml.append("      <description>")
                    .append(generateScreenDescription(i))
                    .append("</description>\n");

            appendFields(xml, i);
            appendActions(xml, i);
            appendValidations(xml, i);

            xml.append("    </screen>\n");
        }

        xml.append("  </screens>\n");
    }

    private void appendFields(StringBuilder xml, int screenIndex) {
        xml.append("      <fields>\n");

        for (int j = 1; j <= 8; j++) {
            xml.append("        <field name=\"field_")
                    .append(screenIndex)
                    .append("_")
                    .append(j)
                    .append("\" label=\"")
                    .append(generateFieldLabel(j))
                    .append("\" required=\"")
                    .append(j % 2 == 0)
                    .append("\" />\n");
        }

        xml.append("      </fields>\n");
    }

    private void appendActions(StringBuilder xml, int screenIndex) {
        xml.append("      <actions>\n");
        xml.append("        <action code=\"CONTINUE_").append(screenIndex).append("\">Devam Et</action>\n");
        xml.append("        <action code=\"CANCEL_").append(screenIndex).append("\">İşlemi İptal Et</action>\n");
        xml.append("        <action code=\"CREATE_CASE_").append(screenIndex).append("\">Kayıt Oluştur</action>\n");
        xml.append("      </actions>\n");
    }

    private void appendValidations(StringBuilder xml, int screenIndex) {
        xml.append("      <validations>\n");
        xml.append("        <validation field=\"customerNo\">Müşteri numarası boş bırakılamaz.</validation>\n");
        xml.append("        <validation field=\"invoiceNo\">Fatura numarası geçerli formatta olmalıdır.</validation>\n");
        xml.append("        <validation field=\"description\">Açıklama alanı işlem sebebini içermelidir.</validation>\n");
        xml.append("      </validations>\n");
    }

    private void appendTransitions(StringBuilder xml, int screenCount) {
        xml.append("  <transitions>\n");

        for (int i = 1; i < screenCount; i++) {
            xml.append("    <transition from=\"SCR_")
                    .append(i)
                    .append("\" to=\"SCR_")
                    .append(i + 1)
                    .append("\">\n");
            xml.append("      <condition>customerVerified == true</condition>\n");
            xml.append("    </transition>\n");
        }

        xml.append("  </transitions>\n");
    }

    private void appendBusinessRules(StringBuilder xml, int ruleCount) {
        xml.append("  <businessRules>\n");

        for (int i = 1; i <= ruleCount; i++) {
            xml.append("    <rule id=\"RULE_").append(i).append("\">\n");
            xml.append("      <name>").append(generateRuleName(i)).append("</name>\n");
            xml.append("      <description>")
                    .append("Müşteri işlem akışında doğrulama, ödeme, fatura, itiraz, abonelik veya arıza koşullarını kontrol eder.")
                    .append("</description>\n");
            xml.append("    </rule>\n");
        }

        xml.append("  </businessRules>\n");
    }

    private String generateScreenTitle(int index) {
        String[] titles = {
                "Müşteri Bilgileri Sorgulama",
                "Kimlik Doğrulama Ekranı",
                "Fatura Detay Ekranı",
                "Fatura İtiraz Formu",
                "Ödeme Durumu Kontrolü",
                "Abonelik İptal Talebi",
                "Arıza Kaydı Oluşturma",
                "İşlem Sonuç Ekranı"
        };

        return titles[(index - 1) % titles.length];
    }

    private String generateScreenDescription(int index) {
        String[] descriptions = {
                "Müşteri numarası veya TC kimlik numarası ile müşteri bilgileri sorgulanır.",
                "Müşteri kimlik bilgileri doğrulanır ve işlem güvenliği kontrol edilir.",
                "Müşterinin son dönem faturası, ödeme durumu ve itiraz edilebilir kalemleri görüntülenir.",
                "Müşteri fatura tutarına itiraz ediyorsa itiraz nedeni ve açıklama bilgisi alınır.",
                "Ödeme başarısızlığı, gecikmiş ödeme ve tahsilat durumu kontrol edilir.",
                "Abonelik iptal talebi için gerekli onay ve cayma bedeli kontrolleri yapılır.",
                "Teknik arıza bildirimi alınır ve ilgili ekip için kayıt oluşturulur.",
                "Yapılan işlemin sonucu müşteri temsilcisine gösterilir."
        };

        return descriptions[(index - 1) % descriptions.length];
    }

    private String generateFieldLabel(int index) {
        String[] labels = {
                "Müşteri Numarası",
                "TC Kimlik Numarası",
                "Telefon Numarası",
                "Fatura Numarası",
                "İtiraz Nedeni",
                "Ödeme Referansı",
                "Açıklama",
                "İşlem Notu"
        };

        return labels[(index - 1) % labels.length];
    }

    private String generateRuleName(int index) {
        String[] names = {
                "Müşteri Doğrulama Kuralı",
                "Fatura İtiraz Uygunluk Kuralı",
                "Ödeme Durumu Kontrol Kuralı",
                "Abonelik İptal Kontrol Kuralı",
                "Arıza Kaydı Yönlendirme Kuralı"
        };

        return names[(index - 1) % names.length];
    }
}
