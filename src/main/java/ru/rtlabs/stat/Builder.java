package ru.rtlabs.stat;

public class Builder {

    public static String codeEnter(String code){
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:for=\"http://www.forus.ru\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <for:GetRegPhys>\n" +
                "         <for:Data>"+code+"</for:Data>\n" +
                "      </for:GetRegPhys>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }
}
