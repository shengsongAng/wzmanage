package com.jysoft.controller;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/query")
public class QueryController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static SimpleDateFormat sdf_month = new SimpleDateFormat("yyyy-MM");

    @RequestMapping("/queryIndex")
    @ResponseBody
    public String queryIndex(@RequestParam(value="org",required=false)String org,@RequestParam(value="desc_wz",required=false)String desc_wz){
        JSONObject obj = null;
        try{
            String sql = "select * from WZ_INFO where 1=1";
            //条件：单位
            if(org!=null && !org.equals("")){
                sql += " and EXCEL_COL_3 like '%"+ org +"%'";
            }
            //条件：物资名称
            if(desc_wz!=null && !desc_wz.equals("")){
                sql += " and WZ_NAME like '%"+ desc_wz +"%'";
            }
            sql += " order by CREATE_DATE desc";
            List<Map<String,Object>> list = jdbcTemplate.queryForList(sql);

            obj=new JSONObject();
            obj.put("data",list);
        }catch (Exception e){
            e.printStackTrace();
        }
        return obj.toString();
    }

    /**
     * 入库查询
     */
    @RequestMapping("/queryStorageDetail")
    @ResponseBody
    public String queryStorageDetail(@RequestParam(value="start",required=false)String start,
                                     @RequestParam(value="end",required=false)String end,
                                     @RequestParam(value="desc_wz",required=false)String desc_wz){
        JSONObject obj = null;
        try{
            String sql = "select * from WZ_OPT_DETAIL where OPT_TYPE IN ('1','2','4')";
            //条件：日期
            if(!StringUtils.isEmpty(start) && !StringUtils.isEmpty(end)){
//                sql += " and  to_char(CREATE_DATE,'yyyy-mm')='"+date+"'";
                sql += " and (to_char(CREATE_DATE,'yyyy-mm') between '"+start+"' and '"+end+"')";
            }
            //条件：物资名称
            if(desc_wz!=null && !desc_wz.equals("")){
                sql += " and WZ_NAME like '%"+ desc_wz +"%'";
            }
            sql += " order by CREATE_DATE desc";
            List<Map<String,Object>> list = jdbcTemplate.queryForList(sql);

            obj=new JSONObject();
            obj.put("data",list);
        }catch (Exception e){
            e.printStackTrace();
        }
        return obj.toString();
    }

    /**
     * 领用查询
     */
    @RequestMapping("/queryUseDetail")
    @ResponseBody
    public String queryUseDetail(@RequestParam(value="start",required=false)String start,
                                 @RequestParam(value="end",required=false)String end,
                                 @RequestParam(value="desc_wz",required=false)String desc_wz){
        JSONObject obj = null;
        try{
            String sql = "select * from WZ_OPT_DETAIL where OPT_TYPE IN ('3','5')";
            //条件：日期
            if(!StringUtils.isEmpty(start) && !StringUtils.isEmpty(end)){
//                sql += " and  to_char(CREATE_DATE,'yyyy-mm')='"+date+"'";
                sql += " and (to_char(CREATE_DATE,'yyyy-mm') between '"+start+"' and '"+end+"')";
            }
            //条件：物资名称
            if(desc_wz!=null && !desc_wz.equals("")){
                sql += " and WZ_NAME like '%"+ desc_wz +"%'";
            }
            sql += " order by CREATE_DATE desc";
            List<Map<String,Object>> list = jdbcTemplate.queryForList(sql);

            obj=new JSONObject();
            obj.put("data",list);
        }catch (Exception e){
            e.printStackTrace();
        }
        return obj.toString();
    }

    /**
     * 根据名称查询所有明细
     */
    @RequestMapping("/queryDetailByName")
    @ResponseBody
    public String queryDetailByName(
//                                    @RequestParam(value="datetype",required=false)String datetype,
                                    @RequestParam(value="start",required=false)String start,
                                    @RequestParam(value="end",required=false)String end,
                                    @RequestParam(value="opt",required=false)String opt,
                                    @RequestParam(value="desc_wz",required=false)String desc_wz){
        JSONObject obj = null;
        try{
            String sql = "select * from WZ_OPT_DETAIL where  WZ_NAME like '%"+ desc_wz +"%'";
            //
//            if(datetype!=null && !datetype.equals("")){
//                if(datetype.equals("currMonth")){//本月
//                    sql += "and to_char(CREATE_DATE,'yyyy-mm')=to_char(sysdate,'yyyy-mm')";
//                }
//                else if(datetype.equals("threeMonth")){//近三月
//                    sql += "and to_char(CREATE_DATE,'yyyy-mm')>to_char(add_months(sysdate,-3),'YYYY-MM')";
//                }
//                else if(datetype.equals("year")){//近一年
//                    sql += "and to_char(CREATE_DATE,'yyyy-mm')>to_char(add_months(sysdate,-12),'YYYY-MM')";
//                }
//            }
            //时间
            if(!StringUtils.isEmpty(start) && !StringUtils.isEmpty(end)){
                sql += " and (to_char(CREATE_DATE,'yyyy-mm') between '"+start+"' and '"+end+"')";
            }
            //操作
            if(opt!=null && !opt.equals("")){
               if(opt.equals("use")){//领用
                   sql += " and OPT_TYPE in ('3','5')";
               }
               else if(opt.equals("repeatUse")){//重复利用
                   sql += " and OPT_TYPE = '4'";
               }
               else if(opt.equals("storage")){//入库
                   sql += " and OPT_TYPE in ('1','2')";
               }
            }
            sql += " order by CREATE_DATE desc";
            List<Map<String,Object>> list = jdbcTemplate.queryForList(sql);

            obj=new JSONObject();
            obj.put("data",list);
        }catch (Exception e){
            e.printStackTrace();
        }
        return obj.toString();
    }

    /**
     * 查询月统计
     */
    @RequestMapping("/queryMonth")
    @ResponseBody
    public String queryMonth(@RequestParam(value="date",required=false)String date,
                             @RequestParam(value="lastMonth",required=false)String lastMonth,
                             @RequestParam(value="desc_wz",required=false)String desc_wz){
        JSONObject result = new JSONObject();
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

            List<Map<String,Object>> list = jdbcTemplate.queryForList(sql);

            result.put("data",list);
        }catch (Exception e){
            e.printStackTrace();
        }

        return result.toString();
    }

    /**
     * 获取所有的项目名称
     */
    @RequestMapping("/queryAllProject")
    @ResponseBody
    public String queryAllProject(@RequestParam(value="desc_wz",required=false)String desc_wz){
        JSONObject result = new JSONObject();
        try{

            String sql = "select distinct PROJECT_NAME from WZ_OPT_DETAIL where WZ_NAME like '%"+desc_wz+"%'";

            List<Map<String,Object>> list = jdbcTemplate.queryForList(sql);

            result.put("data",list);
        }catch (Exception e){
            e.printStackTrace();
        }

        return result.toString();
    }

}
