package ru.rtlabs.service;

import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import ru.rtlabs.Entity.Employe;
import ru.rtlabs.rowmapper.*;
import ru.rtlabs.stat.Builder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import static org.w3c.dom.Node.*;


public class Service {
    private JdbcTemplate jdbcTemplate;
    private String url;
    private String username;
    private String password;
    private Integer orgId;
    private static final Logger LOG =Logger.getLogger(Service.class);
    public void postSend(){
        try {
            String content = Builder.codeEnter(String.valueOf(searchOrgCode()));
            URL url = new URL("http://" + this.username+":"+this.password+"@"+this.url);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
            conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
            conn.setRequestProperty("SOAPAction", "http://www.forus.ru#PassportService:GetRegPhys");
            conn.setRequestProperty("Content-Length", String.valueOf(content.length()));
            conn.setRequestProperty("Host", "guzio.ru:1080");
            conn.setRequestProperty("User-Agent", "Apache-HttpClient/4.1.1 (java 1.5)");
            conn.setRequestProperty("Cookie", "$Version=1; vrs_rc=\"\"");
            conn.setRequestProperty("Cookie2", "$Version=1");
            conn.setRequestProperty("Authorization", "Basic bHB1OmxwdQ==");
            conn.setDoOutput(true);
            OutputStream reqStream = conn.getOutputStream();
            reqStream.write(content.getBytes());
            LOG.info("\nPOST\n" +
                    this.url+"\n" +
                    "Accept-Encoding: " + "gzip,deflate" + "\n" +
                    "Content-Type: " + "text/xml;charset=UTF-8" + "\n" +
                    "SOAPAction: " + "http://www.forus.ru#PassportService:GetRegPhys" + "\n" +
                    "Content-Length: " + String.valueOf(content.length()) + "\n" +
                    "Host: " + "guzio.ru:1080" + "\n" +
                    "User-Agent: " + "Apache-HttpClient/4.1.1 (java 1.5)" + "\n" +
                    "Cookie: " + "$Version=1; vrs_rc=\"\"" + "\n" +
                    "Cookie2: " + "$Version=1" + "\n" +
                    "Authorization: " + "Basic bHB1OmxwdQ==" + "\n" +
                    "Payload: \n" + content);


            String res = null;
            InputStreamReader isr = null;
            if(conn.getResponseCode() == 500){
                isr = new InputStreamReader(conn.getErrorStream(), "UTF-8");
                LOG.warn("Response code: + " + conn.getResponseCode() + " from " + this.url);
            }else if ("gzip".equals(conn.getContentEncoding())){
                isr = new InputStreamReader(new GZIPInputStream(conn.getInputStream()));
            }else {
                isr = new InputStreamReader(conn.getInputStream());
            }

            BufferedReader bfr = new BufferedReader(isr);
            StringBuffer sbf = new StringBuffer();
            int ch = bfr.read();
            while (ch != -1) {
                sbf.append((char) ch);
                ch = bfr.read();
            }
            res = sbf.toString();
            LOG.info("\nResponse code: " + conn.getResponseCode() + "\n" +
                    "Ответ :\n" + res);
            responseParse(res);
        } catch (MalformedURLException e) {
            LOG.error("Сломанный URL - проверьте URL",e);
        } catch (ProtocolException e) {
            LOG.error("Внутр/ошибка протокола",e);
        } catch (IOException e) {
            LOG.error("Ошибка коннекта",e);
        }
    }

    private void responseParse(String response){
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse((new InputSource(new StringReader(response))));
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getChildNodes();
                for (int i = 0; i < nList.getLength(); i++) {
                     Node node = nList.item(i);
                    responseParseData(node.getTextContent(), dbFactory, doc);
            }

        } catch (Exception e) {
            LOG.error("Ошибка", e);
        }
    }

    private void responseParseData(String response, DocumentBuilderFactory dbf, Document doc){
        try {
            List<String> parsed = new ArrayList<>();
            ArrayList<Map<String, String>>  doctors = new ArrayList<>();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            doc = builder.parse(new InputSource(new StringReader(response)));
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("row");
            for (int i = 0; i < nList.getLength(); i++) {
                Node node = nList.item(i);
                parsed.add(node.getTextContent());
            }

            for (String s : parsed) {
               String[] array = s.split("\\n");
                Map<String, String> doca = new HashMap<>();
                //System.out.println(Arrays.toString(array));
                doca.put("Период", array[1].substring(2).substring(0, 10));
                doca.put("ОГРН", array[2].substring(2));
                doca.put("PCode", array[3].substring(2));
                doca.put("СНИЛС", array[4].substring(2));
                doca.put("Фамилия", array[5].substring(2));
                doca.put("Имя", array[6].substring(2));
                doca.put("Отчество", array[7].substring(2));
                doca.put("Должность", array[8].substring(2));
                doca.put("Дата start_date", array[9].substring(2).substring(0, 10));
                doca.put("Дата end_date", array[10].substring(2).substring(0, 10));
                doca.put("Место работы", array[11].substring(2));
                doca.put("OKTMO", array[12].substring(2));
                doctors.add(doca);
            }
            bdUpdate(doctors);
        } catch (Exception e) {
            LOG.error("Ошибка", e);
        }
    }
    private void bdUpdate(ArrayList<Map<String, String>> doctors){
        for (Map<String, String> doctor : doctors) {
            Integer eplId = searchEmpl(search(doctor.get("СНИЛС")));
            Integer positionId = searchPosition(doctor.get("Должность"));
            Integer emplPosition = searchEmplPosition(positionId, eplId);

            System.out.println("id в РМИС " + search(doctor.get("СНИЛС")));
            System.out.println("positionId " + positionId);
            System.out.println("eplId " + eplId);
            System.out.println("emplPosition " + emplPosition);
            System.out.println(doctor.get("Период") + " " + doctor.get("ОГРН")  + " " + doctor.get("PCode") + " " + doctor.get("СНИЛС") + " " + doctor.get("Фамилия") + " " + doctor.get("Имя") + " " + doctor.get("Отчество") + " " + doctor.get("Должность") + " " + doctor.get("Дата start_date") + " " + doctor.get("Дата end_date") + " " + doctor.get("Место работы") + " " + doctor.get("OKTMO"));
            if (!hasRe(emplPosition) && emplPosition != 0){
                recInsert(emplPosition, doctor.get("Дата start_date"), doctor.get("Дата end_date"));
                System.out.println("Врач вставлен в РМИС");

            }else if (emplPosition != 0)
            {
                codeupdate(doctor, emplPosition);
                System.out.println("PCode " + doctor.get("PCode") + " вставлен");
            }else
             {
                System.out.println("Врач уже существует в таблице или не найден в РМИС");
             }

        }
    }

    private void codeupdate(Map<String, String> doctor, Integer emplPositionId)
    {
        String update = "UPDATE PIM_EMPLOYEE_POSITION set code = ? where id = ?";
        jdbcTemplate.update(update, doctor.get("PCode"), emplPositionId);
    }

    public Boolean hasRe(Integer id){
        try {
            String query = "select 1 from pim_empl_posit_to_prefer_recipe where employee_position_id = ? limit 1";

            jdbcTemplate.queryForObject(query, new Object[]{id}, Long.class);
            return true;
        }catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
    public void recInsert(Integer id, String startDate, String endDate){

        try {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date parsedB = format.parse(startDate);
            java.sql.Date sqlD = new java.sql.Date(parsedB.getTime());
            if (!endDate.contains("0001-01-01")){
                DateFormat format2 = new SimpleDateFormat("yyyy-MM-dd");
                Date parsedB2 = format2.parse(endDate);
                java.sql.Date sqlDE = new java.sql.Date(parsedB2.getTime());
                String pass = "insert into pim_empl_posit_to_prefer_recipe (id, employee_position_id, begin_date, end_date) values (nextval('pim_empl_posit_to_prefer_recipe_seq'), ?, ?, ?)";
                jdbcTemplate.update(pass, id, sqlD, sqlDE);
            }else {
                String pass = "insert into pim_empl_posit_to_prefer_recipe (id, employee_position_id, begin_date) values (nextval('pim_empl_posit_to_prefer_recipe_seq'), ?, ?)";
                jdbcTemplate.update(pass, id, sqlD);
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    private Integer search(String snils){
        List<Employe> indiv = null;
        StringBuilder number = new StringBuilder();
        if (snils != null){
            char[] a = snils.toCharArray();
            for (char c : a) {
                if (c != '-'){
                    if (c != ' '){
                        number.append(c);
                    }
                }
            }
        }
        String sql = "SELECT * from pim_individual_doc where number like ?";
        indiv = jdbcTemplate.query(sql, new Object[] {number.toString()}, new DocRowMapper());
        if (indiv.size() == 0 && snils != null){
            indiv = jdbcTemplate.query(sql, new Object[] {snils}, new DocRowMapper());
        }
        return (indiv.size() != 0 ? indiv.get(0).getId() : 0);
    }
    private Integer searchEmpl(Integer id){

        List<Employe> indiv = null;
        String sql = "SELECT * from pim_employee where individual_id = ?";
        indiv = jdbcTemplate.query(sql, new Object[] {id}, new EmployeeRowMapper());
        return (indiv.size() != 0 ? indiv.get(0).getEmplId(): 0);
    }

    private String searchOrgCode(){

        List<Employe> indiv = null;
        String sql = "SELECT * from pim_org_code where type_id = 4 and org_id = ?";
        indiv = jdbcTemplate.query(sql, new Object[] {this.orgId}, new OrganizationRowMapper());
        return indiv.get(0).getOrgCode();
    }
    private Integer searchPosition(String doctor){
        List<Employe> indiv = null;
        String sql = "SELECT * from pim_position WHERE lower(name) LIKE lower(?) and organization_id = ?";
        indiv = jdbcTemplate.query(sql, new Object[] {doctor, this.orgId}, new PostionMapper());
        return (indiv.size() != 0 ? indiv.get(0).getPositionId() : 0);
    }

    private Integer searchEmplPosition(Integer positionId, Integer emplId){
        List<Employe> indiv = null;
        //String sql = "select id from PIM_EMPLOYEE_POSITION where position_id = ? and employee_id = ?";
        String sql = "select id from PIM_EMPLOYEE_POSITION where employee_id = ? limit 1";
        //indiv = jdbcTemplate.query(sql, new Object[] {positionId, emplId}, new PimEplMapper());
        indiv = jdbcTemplate.query(sql, new Object[] {emplId}, new PimEplMapper());
        return (indiv.size() != 0 ? indiv.get(0).getEmplPosition() : 0);
    }


    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getOrgId() {
        return orgId;
    }

    public void setOrgId(Integer orgId) {
        this.orgId = orgId;
    }
}
