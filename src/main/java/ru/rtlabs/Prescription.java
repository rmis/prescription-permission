package ru.rtlabs;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import ru.rtlabs.service.Service;

import java.util.Properties;

public class Prescription {
    private static final Logger LOG =Logger.getLogger(Prescription.class);
    public static void main(String[] args) {


        ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
        Service service = ctx.getBean(Service.class);
        service.setOrgId(Integer.parseInt(args[0]));
        service.postSend();

    }
}

