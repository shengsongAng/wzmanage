package com.jysoft.controller;

import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/download")
public class DownloadController {

    @Autowired
    JdbcTemplate jdbcTemplate;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

    private String[] titleArr_use = {"物资描述","领用数量","领用时间","项目","领用人"};
    private String[] titleArr_storage = {"物资描述","入库数量","类型","入库时间","项目","需求人","退还人"};
    private String[] titleArr_month = {"物资描述","上月库存结余","月入库数","月领用数"};

    private String[] colArr_storage = {"序号","匹配批次号","申请批次号","需求单位","项目描述","采购申请号","行项目","物料号","物料描述","数量",
            "计量单位","反馈意见","框架协议号","框架协议行号","供应商编码","供应商描述","价格联动标识","不含税单价（元）",
            "含税单价（元）","不含税总价（元）","含税总价（元）","原含税单价","原含税总价","项目交货日期","补充特征值","交货地点及交货方式文本",
            "需求人","备注"};

    private String[] colArr_use = {"物料描述","领用数量","领用人","项目","领用时间"};

    //领用明细导出
    @RequestMapping(value = "/downExcelUse", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public void downExcelUse(HttpServletResponse response,
                               @RequestParam(value="date",required=false)String date,
                               @RequestParam(value="desc_wz",required=false)String desc_wz){

        try{
            //查询所有条件的数据
            String sql = "select * from WZ_OPT_DETAIL where OPT_TYPE IN ('3','5')";
            //条件：日期
            if(date!=null && !date.equals("")){
                sql += " and  to_char(CREATE_DATE,'yyyy-mm')='"+date+"'";
            }
            //条件：物资名称
            if(desc_wz!=null && !desc_wz.equals("")){
                sql += " and WZ_NAME like '%"+ desc_wz +"%'";
            }
            List<Map<String,Object>> dataList = jdbcTemplate.queryForList(sql);

            String xlsFile_name = "领用明细-" + sdf.format(new Date()) + ".xlsx";     //输出xls文件名称
            //防止乱码
            xlsFile_name = new String(xlsFile_name.getBytes(), "ISO-8859-1");
            //内存中只创建100个对象
            Workbook wb = new SXSSFWorkbook();
            Sheet sheet = wb.createSheet("领用明细");     //工作表对象
            Row row = null;        //行对象
            Cell cell = null;      //列对象

            //创建标题行
            row = sheet.createRow(0);
            for(int i=0; i<titleArr_use.length; i++){
                cell = row.createCell(i);
                cell.setCellValue(titleArr_use[i]);
            }

            for (int k=0;k<dataList.size();k++) {
                Map<String,Object> m = dataList.get(k);
                String wz_name = m.get("WZ_NAME").toString();
                String use_count = m.get("WZ_STOCK").toString();
                String use_date = m.get("OPT_DATE").toString();
                String pro_name = m.get("PROJECT_NAME").toString();
                String user = m.get("RELATED_PERSON").toString();

                //row要从第二行开始，因为第一行已经初始化为标题
                row = sheet.createRow(k+1);
                row.createCell(0).setCellValue(wz_name);
                row.createCell(1).setCellValue(use_count);
                row.createCell(2).setCellValue(use_date);
                row.createCell(3).setCellValue(pro_name);
                row.createCell(4).setCellValue(user);
            }
            response.setContentType("application/vnd.ms-excel;charset=utf-8");
            response.setHeader("Content-disposition", "attachment;filename=" + xlsFile_name);
            response.flushBuffer();
            OutputStream outputStream = response.getOutputStream();
            wb.write(response.getOutputStream());
            wb.close();
            outputStream.flush();
            outputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    //入库明细导出
    @RequestMapping(value = "/downExcelStorage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public void downExcelStorage(HttpServletResponse response,
                             @RequestParam(value="date",required=false)String date,
                             @RequestParam(value="desc_wz",required=false)String desc_wz){

        try{
            String sql = "select * from WZ_OPT_DETAIL where OPT_TYPE IN ('1','2','4')";
            //条件：日期
            if(date!=null && !date.equals("")){
                sql += " and  to_char(CREATE_DATE,'yyyy-mm')='"+date+"'";
            }
            //条件：物资名称
            if(desc_wz!=null && !desc_wz.equals("")){
                sql += " and WZ_NAME like '%"+ desc_wz +"%'";
            }
            List<Map<String,Object>> dataList = jdbcTemplate.queryForList(sql);

            String xlsFile_name = "入库明细-" + sdf.format(new Date()) + ".xlsx";     //输出xls文件名称
            //防止乱码
            xlsFile_name = new String(xlsFile_name.getBytes(), "ISO-8859-1");
            //内存中只创建100个对象
            Workbook wb = new SXSSFWorkbook();
            Sheet sheet = wb.createSheet("入库明细");     //工作表对象
            Row row = null;        //行对象
            Cell cell = null;      //列对象

            //创建标题行
            row = sheet.createRow(0);
            for(int i=0; i<titleArr_storage.length; i++){
                cell = row.createCell(i);
                cell.setCellValue(titleArr_storage[i]);
            }

            for (int k=0;k<dataList.size();k++) {
                Map<String,Object> m = dataList.get(k);
                String wz_name = m.get("WZ_NAME").toString();
                String use_count = m.get("WZ_STOCK").toString();
                String type = m.get("OPT_TYPE").toString();

                String use_date = m.get("OPT_DATE").toString();
                String pro_name = m.get("PROJECT_NAME").toString();
                String user = m.get("RELATED_PERSON").toString();

                //row要从第二行开始，因为第一行已经初始化为标题
                row = sheet.createRow(k+1);
                row.createCell(0).setCellValue(wz_name);
                row.createCell(1).setCellValue(use_count);
                row.createCell(3).setCellValue(use_date);
                row.createCell(4).setCellValue(pro_name);
                if(type.equals("1")){//批量入库
                    row.createCell(2).setCellValue("批量入库");
                    row.createCell(5).setCellValue(user);
                    row.createCell(6).setCellValue("");
                }else if(type.equals("2")){//入库
                    row.createCell(2).setCellValue("入库");
                    row.createCell(5).setCellValue(user);
                    row.createCell(6).setCellValue("");
                }else{//重复利用
                    row.createCell(2).setCellValue("重复利用");
                    row.createCell(5).setCellValue("");
                    row.createCell(6).setCellValue(user);
                }

            }
            response.setContentType("application/vnd.ms-excel;charset=utf-8");
            response.setHeader("Content-disposition", "attachment;filename=" + xlsFile_name);
            response.flushBuffer();
            OutputStream outputStream = response.getOutputStream();
            wb.write(response.getOutputStream());
            wb.close();
            outputStream.flush();
            outputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    //月统计导出
    @RequestMapping(value = "/downExcelMonth", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public void downExcelMonth(HttpServletResponse response,
                             @RequestParam(value="date",required=false)String date,
                             @RequestParam(value="lastMonth",required=false)String lastMonth,
                             @RequestParam(value="desc_wz",required=false)String desc_wz){

        try{
            String sql = "select ti.wz_name, tr.wz_stock, nvl(tn.rk_num, 0) as storagenum, nvl(tn.ly_num, 0) as usenum";
            sql += " from wz_info ti";
            sql += " left join wz_month_report tr";
            sql += " on ti.wz_name = tr.wz_name";
            sql += " and tr.month = '"+lastMonth+"'";
            sql += " left join (";
            sql += " select wz_name,";
            sql += " sum(case";
            sql += " when opt_type in ('1', '2', '4') then";
            sql += " opt_num";
            sql += " end) rk_num,";
            sql += " sum(case";
            sql += " when opt_type in ('3', '5') then";
            sql += " opt_num";
            sql += " end) ly_num";
            sql += " from wz_opt_detail";
            sql += " where to_char(opt_date, 'yyyy-mm') = '"+date+"'";
            sql += " group by wz_name) tn";
            sql += " on tn.wz_name = ti.wz_name";
            if(desc_wz!=null && !"".equals(desc_wz)){
                sql += " where ti.wz_name like '%"+desc_wz+"%'";
            }
            List<Map<String,Object>> dataList = jdbcTemplate.queryForList(sql);

            String xlsFile_name = "月统计-" + sdf.format(new Date()) + ".xlsx";     //输出xls文件名称
            //防止乱码
            xlsFile_name = new String(xlsFile_name.getBytes(), "ISO-8859-1");
            //内存中只创建100个对象
            Workbook wb = new SXSSFWorkbook();
            Sheet sheet = wb.createSheet("月统计");     //工作表对象
            Row row = null;        //行对象
            Cell cell = null;      //列对象

            //创建标题行
            row = sheet.createRow(0);
            for(int i=0; i<titleArr_month.length; i++){
                cell = row.createCell(i);
                cell.setCellValue(titleArr_month[i]);
            }

            for (int k=0;k<dataList.size();k++) {
                Map<String,Object> m = dataList.get(k);
//                {"物资描述","上月库存结余","月入库数","月领用数"};
                String wz_name = m.get("WZ_NAME").toString();
                String wz_stock = m.get("WZ_STOCK")==null ? "" : m.get("WZ_STOCK").toString();
                String storagenum = m.get("STORAGENUM")==null ? "" : m.get("STORAGENUM").toString();
                String usenum = m.get("USENUM")==null ? "" : m.get("USENUM").toString();

                //row要从第二行开始，因为第一行已经初始化为标题
                row = sheet.createRow(k+1);
                row.createCell(0).setCellValue(wz_name);
                row.createCell(1).setCellValue(wz_stock);
                row.createCell(2).setCellValue(storagenum);
                row.createCell(3).setCellValue(usenum);
            }
            response.setContentType("application/vnd.ms-excel;charset=utf-8");
            response.setHeader("Content-disposition", "attachment;filename=" + xlsFile_name);
            response.flushBuffer();
            OutputStream outputStream = response.getOutputStream();
            wb.write(response.getOutputStream());
            wb.close();
            outputStream.flush();
            outputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //下载模板
    @RequestMapping(value = "/downExcelModel", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public void downExcelModel(HttpServletResponse response,
                             @RequestParam(value="flag",required=false)String flag){

        try{

            String[] arr = colArr_use;
            String xlsFile_name = "批量领用模板-" + sdf.format(new Date()) + ".xlsx";     //输出xls文件名称
            if("storage".equals(flag)){
                xlsFile_name = "批量导入模板-" + sdf.format(new Date()) + ".xlsx";
                arr = colArr_storage;
            }
            //防止乱码
            xlsFile_name = new String(xlsFile_name.getBytes(), "ISO-8859-1");

            Workbook wb = new SXSSFWorkbook();
            Sheet sheet = wb.createSheet("工作表");     //工作表对象
            Row row = null;        //行对象
            Cell cell = null;      //列对象

            //创建标题行
            row = sheet.createRow(0);
            for(int i=0; i<arr.length; i++){
                cell = row.createCell(i);
                cell.setCellValue(arr[i]);
            }

            response.setContentType("application/vnd.ms-excel;charset=utf-8");
            response.setHeader("Content-disposition", "attachment;filename=" + xlsFile_name);
            response.flushBuffer();
            OutputStream outputStream = response.getOutputStream();
            wb.write(response.getOutputStream());
            wb.close();
            outputStream.flush();
            outputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
