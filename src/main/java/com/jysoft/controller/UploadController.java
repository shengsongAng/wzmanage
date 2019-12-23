package com.jysoft.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.functions.Rows;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/file")
public class UploadController {

    private String[] titleArr = {"序号","匹配批次号","申请批次号","需求单位","项目描述","采购申请号","行项目","物料号","物料描述","数量",
            "计量单位","反馈意见","框架协议号","框架协议行号","供应商编码","供应商描述","价格联动标识","不含税单价（元）",
            "含税单价（元）","不含税总价（元）","含税总价（元）","原含税单价","原含税总价","项目交货日期","补充特征值","交货地点及交货方式文本",
            "需求人","备注"};

    private String[] titleArr_use = {"物料描述","领用数量","领用人","项目","领用时间"};

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @RequestMapping("/uploadFile")
    @ResponseBody
    public String  uploadFile(@RequestParam("file") MultipartFile file){
        //返回的结果
        JSONObject result = new JSONObject();
        String msg = "导入成功";
        int code = 0;
        result.put("code",code);
        result.put("msg",msg);
        //数据验证的结果，放在result中
        List<JSONObject> resultList = new ArrayList<JSONObject>();
        //计数器，计算执行总数
        Integer total = 0;
        //计数器，计算执行错误总数
        Integer failTotal = 0;
        //
        String fileName = "";

        try {

            fileName = file.getOriginalFilename();

            Workbook workbook = null;
            InputStream is = file.getInputStream();
            if (fileName.endsWith("xls")) {
                workbook = new HSSFWorkbook(is);
            } else if (fileName.endsWith("xlsx")) {
                workbook = new XSSFWorkbook(is);
            }

            if(workbook != null){

                Sheet sheet = workbook.getSheetAt(0);
                //遍历所有行
                int rowCount = sheet.getLastRowNum() + 1;

                for(int rowNum=0; rowNum<rowCount; rowNum++){
                    Row row = sheet.getRow(rowNum);
                    if(rowNum == 0){//第一行，判断标题栏是否对应
                        boolean valideTitle = valideTitle(row,result,"");
                        if(!valideTitle){
                            //标题栏不对，直接跳出
                            break;
                        }
                    }else{
                        //处理行
                        boolean flag = this.validRow(row,resultList);
                        if(!flag){
                            failTotal ++;
                        }
                        total ++;
                    }
                }

            }

            //==========================保存文件到服务器
                // 文件保存路径
                String filePath = System.getProperty("user.dir")+"/upload/";
                // 文件重命名，防止重复
                fileName = filePath + UUID.randomUUID() + fileName;
                // 文件对象
                File dest = new File(fileName);
                // 判断路径是否存在，如果不存在则创建
                if(!dest.getParentFile().exists()) {
                    dest.getParentFile().mkdirs();
                }

                // 保存到服务器中
                file.transferTo(dest);

        } catch (Exception e) {
            e.printStackTrace();
        }

        result.put("data",resultList);
        result.put("total",total);
        result.put("failTotal",failTotal);
        result.put("fileName",fileName);
        return result.toString();
    }

    //处理行Row
    public boolean validRow(Row row,List<JSONObject> resultList){
        boolean flag = true;
        String disc_wz = getCellValue(row.getCell(8));//物资描述
        String count = getCellValue(row.getCell(9));//数量
        String jldw = getCellValue(row.getCell(10));//计量单位

        //查看是否存在当前map中
        JSONObject obj;
        //查询是否存在该物资
        List<Map<String, Object>> list = this.getDataByKeyword(disc_wz,"WZ_NAME","WZ_INFO");
        float currCount = 0;//当前库存
        String currUnit = "";//数据库的单位

        String validRes = "success";//校验结果
        if(list.size()>0){
            currCount = Float.valueOf(list.get(0).get("WZ_STOCK").toString());
            currUnit = list.get(0).get("UNIT").toString();
        }

        obj = new JSONObject();
        obj.put("name",disc_wz);
        obj.put("currCount",currCount);
        obj.put("currUnit",currUnit);
        obj.put("thisCount",count);
        obj.put("thisUnit",jldw);
        if(!"".equals(currUnit.trim()) && !currUnit.trim().equals(jldw.trim())){//单位不匹配
            validRes = "失败，单位不一致";
            flag = false;
        }
        obj.put("validRes",validRes);

        //存放到resultList中
        resultList.add(obj);

        return flag;
    }

    //开始导入数据
    @RequestMapping("/importData")
    @ResponseBody
    public String importData(HttpServletRequest request,@RequestParam("fileName") String fileName){
        //返回的结果
        JSONObject result = new JSONObject();
        String msg = "导入成功";
        int code = 0;
        result.put("code",code);
        result.put("msg",msg);

        Map<String,JSONObject> map = new HashMap<String,JSONObject>();

        try {

            Workbook workbook = null;
            InputStream is = new FileInputStream(fileName);
            if (fileName.endsWith("xls")) {
                workbook = new HSSFWorkbook(is);
            } else if (fileName.endsWith("xlsx")) {
                workbook = new XSSFWorkbook(is);
            }

            if(workbook != null){

                //工号
                HttpSession session = request.getSession();
                String number = session.getAttribute("usernumber")==null ? "" : session.getAttribute("usernumber").toString();

                Sheet sheet = workbook.getSheetAt(0);
                //遍历所有行
                int rowCount = sheet.getLastRowNum() + 1;

                //从1开始，第一行是标题，不用保存
                for(int rowNum=1; rowNum<rowCount; rowNum++){

                    Row row = sheet.getRow(rowNum);
                    //处理行,保存到表detail中，同时同步到info
                    this.handleRow(row,number);
                }

            }

        } catch (Exception e) {
            result.put("code",-1);
            e.printStackTrace();
        }

        return result.toString();
    }

    //处理行Row
    public void handleRow(Row row,String number) throws Exception{
        UUID id = UUID.randomUUID();
        String pch = getCellValue(row.getCell(1));//匹配批次号
        String sqpch = getCellValue(row.getCell(2));//审批匹配批次号
        String org = getCellValue(row.getCell(3));//需求单位
        String disc_pro = getCellValue(row.getCell(4));//项目描述
        String num_cg = getCellValue(row.getCell(5));//采购申请号
        String hxm = getCellValue(row.getCell(6));//行项目
        String wlh =getCellValue(row.getCell(7));//物料号
        String disc_wz = getCellValue(row.getCell(8));//物资描述
        String count = getCellValue(row.getCell(9));//数量
        String jldw = getCellValue(row.getCell(10));//计量单位
        String feedback = getCellValue(row.getCell(11));//反馈意见
        String kjxyh = getCellValue(row.getCell(12));//框架协议号
        String kjxyhh = getCellValue(row.getCell(13));//框架协议行号
        String num_gys = getCellValue(row.getCell(14));//供应商编码
        String disc_gys = getCellValue(row.getCell(15));//供应商描述
        String jgldbs = getCellValue(row.getCell(16));//价格联动标识
        String bhsdj = getCellValue(row.getCell(17));//不含税单价
        String hsdj = getCellValue(row.getCell(18));//含税单价
        String bhszj = getCellValue(row.getCell(19));//不含税总价
        String hszj = getCellValue(row.getCell(20));//含税总价
        String yhsdj = getCellValue(row.getCell(21));//原含税单价
        String yhszj = getCellValue(row.getCell(22));//原含税总价
        String date_jh = getCellValue(row.getCell(23));//交货日期
        String bctzz = getCellValue(row.getCell(24));//补充特征值
        String jhfs = getCellValue(row.getCell(25));//交货方式
        String user = getCellValue(row.getCell(26));//需求人
        String remark = getCellValue(row.getCell(27));//备注

        //查询是否存在该物资
        List<Map<String, Object>> list = this.getDataByKeyword(disc_wz,"WZ_NAME","WZ_INFO");
        float currCount = 0;//当前库存
        String currUnit = "";//数据库的单位
        float count_new = Float.valueOf(count);

        if(list.size()>0){
            currCount = Float.valueOf(list.get(0).get("WZ_STOCK").toString());
            currUnit = list.get(0).get("UNIT").toString();

            if(!currUnit.trim().equals(jldw.trim())){//单位不匹配
                return;
            }else{
                count_new = currCount + count_new;//相加
            }
        }

        String nowDate = sdf.format(new Date());

        //开始入库detail------------------------------------------------
        String sql = "insert into WZ_OPT_DETAIL (ID,WZ_ID,PROJECT_NAME,RELATED_PERSON,WZ_NAME,OPT_TYPE,OPT_DATE,OPT_NUM,WZ_STOCK,OPT_BY,MARK";
            sql += ",EXCEL_COL_1,EXCEL_COL_2,EXCEL_COL_3,EXCEL_COL_4,EXCEL_COL_5,EXCEL_COL_6,EXCEL_COL_7,EXCEL_COL_8,EXCEL_COL_9,EXCEL_COL_10,EXCEL_COL_11";
            sql += ",EXCEL_COL_12,EXCEL_COL_13,EXCEL_COL_14,EXCEL_COL_15,EXCEL_COL_16,EXCEL_COL_17,EXCEL_COL_18,EXCEL_COL_19,EXCEL_COL_20,EXCEL_COL_21,CREATE_DATE)";
            sql += " values('";
            sql += id.toString()+"'";
            sql += ",'"+wlh+"'";
            if(StringUtils.isEmpty(disc_pro)){
                disc_pro = "抢修";
            }
            sql += ",'"+disc_pro+"'";
            sql += ",'"+user+"'";
            sql += ",'"+disc_wz+"'";
            sql += ",'1'";
            sql += ",to_date('"+nowDate+"','yyyy-mm-dd hh24:mi:ss')";
            sql += ","+Float.valueOf(count);
            sql += ","+count_new;
            sql += ",'"+number+"'";//操作工号
            String flag = "需求人："+user+"，项目名称："+disc_pro;
            sql += ",'"+flag+"'";
            sql += ",'"+pch+"'";
            sql += ",'"+sqpch+"'";
            sql += ",'"+org+"'";
            sql += ",'"+num_cg+"'";
            sql += ",'"+hxm+"'";
            sql += ",'"+feedback+"'";
            sql += ",'"+kjxyh+"'";
            sql += ",'"+kjxyhh+"'";
            sql += ",'"+num_gys+"'";
            sql += ",'"+disc_gys+"'";
            sql += ",'"+jgldbs+"'";
            sql += ",'"+bhsdj+"'";
            sql += ",'"+hsdj+"'";
            sql += ",'"+bhszj+"'";
            sql += ",'"+hszj+"'";
            sql += ",'"+yhsdj+"'";
            sql += ",'"+yhszj+"'";
            sql += ",'"+date_jh+"'";
            sql += ",'"+bctzz+"'";
            sql += ",'"+jhfs+"'";
            sql += ",'"+remark+"'";
            sql += ",to_date('"+nowDate+"','yyyy-mm-dd hh24:mi:ss')";
            sql += ")";

            jdbcTemplate.update(sql);
            //入库完成
            //开始同步info-------------------------------------------
            float count_total = 0f;
            String sql_info = "";

            List<Map<String, Object>> list_info = this.getDataByKeyword(disc_wz,"WZ_NAME","WZ_INFO");
            if(list_info.size()>0){
                float count_1 = Float.valueOf(list_info.get(0).get("WZ_STOCK").toString());
                float count_2 = Float.valueOf(count);
                //入库，加操作
                count_total = count_1 + count_2;
                sql_info += "update WZ_INFO ";
                sql_info += "set WZ_STOCK="+count_total;
                sql_info += "where WZ_NAME like '%"+disc_wz+"%'";
            }else{
                UUID id_info = UUID.randomUUID();
                count_total = Float.valueOf(count);
                sql_info += "insert into WZ_INFO values(";
                sql_info += "'"+ id_info.toString() +"'";
                sql_info += ",'"+ disc_wz +"'";
                sql_info += ","+count_total;
                sql_info += ",'"+jldw+"'";
                sql_info += ",to_date('"+nowDate+"','yyyy-mm-dd hh24:mi:ss')";
                sql_info += ",'"+number+"'";
                sql_info += ")";
            }

            jdbcTemplate.update(sql_info);


    }

    @RequestMapping("/uploadFile_use")
    @ResponseBody
    public String  uploadFile_use(@RequestParam("file") MultipartFile file){
        //返回的结果
        JSONObject result = new JSONObject();
        String msg = "导入成功";
        int code = 0;
        result.put("code",code);
        result.put("msg",msg);
        //数据验证的结果，放在result中
        List<JSONObject> resultList = new ArrayList<JSONObject>();
        //计数器，计算执行总数
        Integer total = 0;
        //计数器，计算执行错误总数
        Integer failTotal = 0;
        //
        String fileName = "";

        try {

            fileName = file.getOriginalFilename();

            Workbook workbook = null;
            InputStream is = file.getInputStream();
            if (fileName.endsWith("xls")) {
                workbook = new HSSFWorkbook(is);
            } else if (fileName.endsWith("xlsx")) {
                workbook = new XSSFWorkbook(is);
            }

            if(workbook != null){

                Sheet sheet = workbook.getSheetAt(0);
                //遍历所有行
                int rowCount = sheet.getLastRowNum() + 1;

                for(int rowNum=0; rowNum<rowCount; rowNum++){
                    Row row = sheet.getRow(rowNum);
                    if(rowNum == 0){//第一行，判断标题栏是否对应
                        boolean valideTitle = valideTitle(row,result,"use");
                        if(!valideTitle){
                            //标题栏不对，直接跳出
                            break;
                        }
                    }else{
                        //处理行
                        boolean flag = this.validRow_use(row,resultList);
                        if(flag){
                            failTotal ++;
                        }
                        total ++;
                    }
                }

            }

            //==========================保存文件到服务器
            // 文件保存路径
            String filePath = System.getProperty("user.dir")+"/upload/";
            // 文件重命名，防止重复
            fileName = filePath + UUID.randomUUID() + fileName;
            // 文件对象
            File dest = new File(fileName);
            // 判断路径是否存在，如果不存在则创建
            if(!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }

            // 保存到服务器中
            file.transferTo(dest);

        } catch (Exception e) {
            e.printStackTrace();
        }

        result.put("data",resultList);
        result.put("total",total);
        result.put("failTotal",failTotal);
        result.put("fileName",fileName);
        return result.toString();
    }

    //验证行Row
    public boolean validRow_use(Row row,List<JSONObject> resultList){
        boolean flag = true;
        String disc_wz = getCellValue(row.getCell(0));//物资描述
        String count = getCellValue(row.getCell(1));//数量

        //查看是否存在当前map中
        JSONObject obj;
        //查询是否存在该物资
        List<Map<String, Object>> list = this.getDataByKeyword(disc_wz,"WZ_NAME","WZ_INFO");
        float currCount = 0;//当前库存
        String currUnit = "";//数据库的单位

        String validRes = "success";//校验结果
        if(list.size()>0){
            currCount = Float.valueOf(list.get(0).get("WZ_STOCK").toString());
            currUnit = list.get(0).get("UNIT").toString();
        }

        obj = new JSONObject();
        obj.put("name",disc_wz);
        obj.put("currCount",currCount);
        obj.put("currUnit",currUnit);
        obj.put("thisCount",count);
        if(currCount<Float.valueOf(count)){
            validRes = "库存不足";
            flag = false;
        }
        obj.put("validRes",validRes);

        //存放到map中
        resultList.add(obj);

        return flag;
    }

    //开始导入数据
    @RequestMapping("/importData_use")
    @ResponseBody
    public String importData_use(HttpServletRequest request, @RequestParam("fileName") String fileName){
        //返回的结果
        JSONObject result = new JSONObject();
        String msg = "导入成功";
        int code = 0;
        result.put("code",code);
        result.put("msg",msg);

        Map<String,JSONObject> map = new HashMap<String,JSONObject>();

        try {

            Workbook workbook = null;
            InputStream is = new FileInputStream(fileName);
            if (fileName.endsWith("xls")) {
                workbook = new HSSFWorkbook(is);
            } else if (fileName.endsWith("xlsx")) {
                workbook = new XSSFWorkbook(is);
            }

            //工号
            HttpSession session = request.getSession();
            String number = session.getAttribute("usernumber")==null ? "" : session.getAttribute("usernumber").toString();

            if(workbook != null){

                Sheet sheet = workbook.getSheetAt(0);
                //遍历所有行
                int rowCount = sheet.getLastRowNum() + 1;

                //从1开始，第一行是标题，不用保存
                for(int rowNum=1; rowNum<rowCount; rowNum++){

                    Row row = sheet.getRow(rowNum);
                    //处理行
//                    this.handleRow_use(row,map,number);
                    this.handleRow_use(row,number);
                }
                //开始保存数据到info
//                String nowDate = sdf.format(new Date());
//                Set<String> keys = map.keySet();
//                for(String key:keys){
//                    JSONObject obj = map.get(key);
//                    float count_total = 0f;
//                    String sql = "";
//
//                    List<Map<String, Object>> list = this.getDataByKeyword(key,"WZ_NAME","WZ_INFO");
//                    if(list.size()>0){
//                        float count_1 = Float.valueOf(list.get(0).get("WZ_STOCK").toString());
//                        float count_2 = obj.getFloat("thisCount");
//                        if(count_1<count_2){//库存不足
//                            break;
//                        }
//                        //领用，减操作
//                        count_total = count_1 - count_2;
//                        sql += "update WZ_INFO ";
//                        sql += "set WZ_STOCK="+count_total;
//                        sql += " where WZ_NAME like '%"+key+"%'";
//                    }else{//库存没有不执行
//                        break;
//                    }
//
//                    jdbcTemplate.update(sql);
//                }

            }

        } catch (Exception e) {
            result.put("code",-1);
            e.printStackTrace();
        }

        return result.toString();
    }

    //处理行Row
    public void handleRow_use(Row row,String number) throws Exception{
        UUID id = UUID.randomUUID();
        String desc_wz = getCellValue(row.getCell(0));//物资名
        String count = getCellValue(row.getCell(1));//领用数
        String user = getCellValue(row.getCell(2));//领用人
        String desc_pro = getCellValue(row.getCell(3));//项目
        String use_time = getCellValue(row.getCell(4));//领用时间

        //查询是否存在该物资
        List<Map<String, Object>> list = this.getDataByKeyword(desc_wz,"WZ_NAME","WZ_INFO");
        float count_new = Float.valueOf(count);
        float currCount = 0;

        //判断库存是否做够
        if(list.size()==0){
            return;
        }else{
            currCount = Float.valueOf(list.get(0).get("WZ_STOCK").toString());
            if(Float.valueOf(count)>currCount){
                return;
            }
        }
        count_new = currCount - count_new;
        String nowDate = sdf.format(new Date());

        //开始入库detail--------------------------------------
        String sql = "insert into WZ_OPT_DETAIL (ID,PROJECT_NAME,RELATED_PERSON,WZ_NAME,OPT_TYPE,OPT_DATE,OPT_NUM,WZ_STOCK，OPT_BY ,EXCEL_COL_1,MARK,CREATE_DATE)";
        sql += " values('";
        sql += id.toString()+"'";
        if(StringUtils.isEmpty(desc_pro)){
            desc_pro = "抢修";
        }
        sql += ",'"+desc_pro+"'";
        sql += ",'"+user+"'";
        sql += ",'"+desc_wz+"'";
        sql += ",'5'";
        sql += ",to_date('"+nowDate+"','yyyy-mm-dd hh24:mi:ss')";
        sql += ",-"+Float.valueOf(count);//领用用“-”
        sql += ","+count_new;
        sql += ",'"+number+"'";//操作工号
        sql += ",'"+use_time+"'";
        String flag = "领用人："+user+"，项目名称："+desc_pro;
        sql += ",'"+flag+"'";
        sql += ",to_date('"+nowDate+"','yyyy-mm-dd hh24:mi:ss')";
        sql += ")";

        jdbcTemplate.update(sql);

        //开始同步info----------------------------------
        float count_total = 0f;
        String sql_info = "";

        List<Map<String, Object>> list_info = this.getDataByKeyword(desc_wz,"WZ_NAME","WZ_INFO");
        if(list_info.size()>0){
            float count_1 = Float.valueOf(list_info.get(0).get("WZ_STOCK").toString());
            float count_2 = Float.valueOf(count);
            if(count_1<count_2){//库存不足
                return;
            }
            //领用，减操作
            count_total = count_1 - count_2;
            sql_info += "update WZ_INFO ";
            sql_info += "set WZ_STOCK="+count_total;
            sql_info += " where WZ_NAME like '%"+desc_wz+"%'";
        }else{//库存没有不执行
            return;
        }

        jdbcTemplate.update(sql_info);

        //保存数据到map---------------------------------
//        JSONObject obj = new JSONObject();
//        if(map.containsKey(desc_wz)){
//            obj = map.get(desc_wz);
//            count_new = obj.getFloat("thisCount") + count_new;
//        }
//        obj.put("thisCount",count_new);
//        obj.put("wzname",desc_wz);
//        map.put(desc_wz,obj);
    }

    //拼接SQL，执行查询
    public List<Map<String, Object>> getDataByKeyword(String key,String column,String table){
        String sql = "select * from " + table + " where " + column + " = '" + key + "'";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        return list;
    }

    //验证标题栏
    public boolean valideTitle(Row row,JSONObject result,String type){
        String[] arr = titleArr;
        if(type.equals("use")){//批量领用
            arr = titleArr_use;
        }
        for(int i=0; i<arr.length; i++){
            String title = arr[i];
            String cellValue = row.getCell(i).getStringCellValue();

            if(!cellValue.contains(title)){
                result.put("msg","Excel文件的标题栏对应不正确");
                result.put("code",-1);
                return false;
            }
        }
        return true;
    }

    //判断cell类型，并取值
    public String getCellValue(Cell cell) {
        String cellValue = "";
        // 以下是判断数据的类型
        if(cell == null){
            return "";
        }
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_NUMERIC: // 数字
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    cellValue = sdf.format(org.apache.poi.ss.usermodel.DateUtil.getJavaDate(cell.getNumericCellValue())).toString();
                } else {
                    DataFormatter dataFormatter = new DataFormatter();
                    cellValue = dataFormatter.formatCellValue(cell);
                }
                break;
            case Cell.CELL_TYPE_STRING: // 字符串
                cellValue = cell.getStringCellValue();
                break;
            case Cell.CELL_TYPE_BOOLEAN: // Boolean
                cellValue = cell.getBooleanCellValue() + "";
                break;
            case Cell.CELL_TYPE_FORMULA: // 公式
                cellValue = cell.getCellFormula() + "";
                break;
            case Cell.CELL_TYPE_BLANK: // 空值
                cellValue = "";
                break;
            case Cell.CELL_TYPE_ERROR: // 故障
                cellValue = "非法字符";
                break;
            default:
                cellValue = "未知类型";
                break;
        }
        return cellValue;
    }

    //单个入库
    @RequestMapping("/storage")
    @ResponseBody
    public String storage(HttpServletRequest request,@RequestParam("desc_wz")String desc_wz,@RequestParam("desc_pro")String desc_pro,
                          @RequestParam("user")String user,@RequestParam("stock")String stock,@RequestParam("count")String count){

        //添加记录detail
        String id = UUID.randomUUID().toString();

        String nowDate = sdf.format(new Date());

        //工号
        HttpSession session = request.getSession();
        String number = session.getAttribute("usernumber")==null ? "" : session.getAttribute("usernumber").toString();

        String sql = "insert into WZ_OPT_DETAIL (ID,WZ_NAME,PROJECT_NAME,RELATED_PERSON，OPT_NUM,WZ_STOCK,OPT_TYPE,OPT_DATE,CREATE_DATE，MARK,OPT_BY) values (";
            sql += "'"+id+"'";
            sql += ",'"+desc_wz+"'";
            if(StringUtils.isEmpty(desc_pro)){
                desc_pro = "抢修";
            }
            sql += ",'"+desc_pro+"'";
            sql += ",'"+user+"'";
            sql += ","+Float.valueOf(count)+"";
            sql += ","+stock;
            sql += ",'2'";
            sql += ",to_date('"+nowDate+"','yyyy-mm-dd hh24:mi:ss')";
            sql += ",to_date('"+nowDate+"','yyyy-mm-dd hh24:mi:ss')";
            String flag = "需求人："+user+"，项目名称："+desc_pro;
            sql += ",'"+flag+"'";
            sql += ",'"+number+"'";
            sql += ")";

        jdbcTemplate.update(sql);

        //更改统计表info
        String sql_1 = "select * from WZ_INFO where WZ_NAME like '%"+desc_wz+"%'";
        List<Map<String,Object>> list = jdbcTemplate.queryForList(sql_1);
        if(list.size()>0){
            float currCount = Float.valueOf(list.get(0).get("WZ_STOCK").toString());
            float count_new = currCount + Float.valueOf(count);
            String sql_2 = "update WZ_INFO set WZ_STOCK="+count_new+" where WZ_NAME like '%"+desc_wz+"%'";
            jdbcTemplate.update(sql_2);
        }

        JSONObject result = new JSONObject();
        result.put("code",0);
        return result.toString();
    }

    //单个领用
    @RequestMapping("/wz_use")
    @ResponseBody
    public String wz_use(HttpServletRequest request,@RequestParam("desc_wz")String desc_wz,@RequestParam("desc_pro")String desc_pro,
                         @RequestParam("user")String user,@RequestParam("stock")String stock,@RequestParam("count")String count){

        //添加记录info
        String id = UUID.randomUUID().toString();

        String nowDate = sdf.format(new Date());

        //工号
        HttpSession session = request.getSession();
        String number = session.getAttribute("usernumber")==null ? "" : session.getAttribute("usernumber").toString();

        String sql = "insert into WZ_OPT_DETAIL (ID,WZ_NAME,PROJECT_NAME,RELATED_PERSON，OPT_NUM,WZ_STOCK,OPT_TYPE,OPT_DATE,CREATE_DATE,MARK,OPT_BY) values (";
        sql += "'"+id+"'";
        sql += ",'"+desc_wz+"'";
        if(StringUtils.isEmpty(desc_pro)){
            desc_pro = "抢修";
        }
        sql += ",'"+desc_pro+"'";
        sql += ",'"+user+"'";
        sql += ","+(0-Float.valueOf(count))+"";
        sql += ","+stock;
        sql += ",'3'";
        sql += ",to_date('"+nowDate+"','yyyy-mm-dd hh24:mi:ss')";
        sql += ",to_date('"+nowDate+"','yyyy-mm-dd hh24:mi:ss')";
        String flag = "领用人："+user+"，项目名称："+desc_pro;
        sql += ",'"+flag+"'";
        sql += ",'"+number+"'";
        sql += ")";

        jdbcTemplate.update(sql);

        //更改统计表
        String sql_1 = "select * from WZ_INFO where WZ_NAME like '%"+desc_wz+"%'";
        List<Map<String,Object>> list = jdbcTemplate.queryForList(sql_1);
        if(list.size()>0){
            float currCount = Float.valueOf(list.get(0).get("WZ_STOCK").toString());
            float count_new = currCount - Float.valueOf(count);
            String sql_2 = "update WZ_INFO set WZ_STOCK="+count_new+" where WZ_NAME like '%"+desc_wz+"%'";
            jdbcTemplate.update(sql_2);
        }

        JSONObject result = new JSONObject();
        result.put("code",0);
        return result.toString();
    }

    //重复利用
    @RequestMapping("/repeat_use")
    @ResponseBody
    public String repeat_use(HttpServletRequest request,@RequestParam("desc_wz")String desc_wz,@RequestParam("desc_pro")String desc_pro,
                             @RequestParam("user")String user,@RequestParam("count")String count,@RequestParam("stock")String stock){

        //添加记录info
        String id = UUID.randomUUID().toString();

        String nowDate = sdf.format(new Date());

        //工号
        HttpSession session = request.getSession();
        String number = session.getAttribute("usernumber")==null ? "" : session.getAttribute("usernumber").toString();

        String sql = "insert into WZ_OPT_DETAIL (ID,WZ_NAME,PROJECT_NAME,RELATED_PERSON，OPT_NUM,WZ_STOCK,OPT_TYPE,OPT_DATE,CREATE_DATE,MARK,OPT_BY) values (";
        sql += "'"+id+"'";
        sql += ",'"+desc_wz+"'";
        if(StringUtils.isEmpty(desc_pro)){
            desc_pro = "抢修";
        }
        sql += ",'"+desc_pro+"'";
        sql += ",'"+user+"'";
        sql += ","+Float.valueOf(count)+"";
        sql +=","+stock;
        sql += ",'4'";
        sql += ",to_date('"+nowDate+"','yyyy-mm-dd hh24:mi:ss')";
        sql += ",to_date('"+nowDate+"','yyyy-mm-dd hh24:mi:ss')";
        String flag = "重复利用操作人："+user+"，项目名称："+desc_pro;
        sql += ",'"+flag+"'";
        sql += ",'"+number+"'";
        sql += ")";

        jdbcTemplate.update(sql);

        //更改统计表
        String sql_1 = "select * from WZ_INFO where WZ_NAME like '%"+desc_wz+"%'";
        List<Map<String,Object>> list = jdbcTemplate.queryForList(sql_1);
        if(list.size()>0){
            float currCount = Float.valueOf(list.get(0).get("WZ_STOCK").toString());
            float count_new = currCount + Float.valueOf(count);
            String sql_2 = "update WZ_INFO set WZ_STOCK="+count_new+" where WZ_NAME like '%"+desc_wz+"%'";
            jdbcTemplate.update(sql_2);
        }

        JSONObject result = new JSONObject();
        result.put("code",0);
        return result.toString();
    }
}
