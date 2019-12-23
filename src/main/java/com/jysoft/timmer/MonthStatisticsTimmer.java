package com.jysoft.timmer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class MonthStatisticsTimmer {
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private SimpleDateFormat sdf_month = new SimpleDateFormat("yyyy-MM");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    //每月1号0点
    @Scheduled(cron="0 0 0 1 * ?")
//    @Scheduled(cron="30 * * * * ?")
    public void start(){
        try{

            //获取上个月
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH,-1);
            String date = sdf_month.format(calendar.getTime());

            System.out.println("---------"+date+",同步数据开始---------");
            //从info表查询
            String sql = "select * from WZ_INFO where to_char(CREATE_DATE,'yyyy-mm') = '" + date +"'";

            List<Map<String,Object>> list = jdbcTemplate.queryForList(sql);

            for(int i=0; i<list.size(); i++){
                Map<String,Object> map = list.get(i);
                String id = UUID.randomUUID().toString();
                String WZ_NAME = map.get("WZ_NAME").toString();
                String WZ_STOCK = map.get("WZ_STOCK").toString();
                String UNIT = map.get("UNIT").toString();
                String CREATE_DATE = sdf.format(sdf.parse(map.get("CREATE_DATE").toString()));
                String CREATE_BY = map.get("CREATE_BY").toString();

                String sql_1 = "insert into WZ_MONTH_REPORT values(";
                    sql_1 += "'"+id+"'";
                    sql_1 += ",'"+WZ_NAME+"'";
                    sql_1 += ",'"+date+"'";
                    sql_1 += ","+Float.valueOf(WZ_STOCK)+"";
                    sql_1 += ",'"+UNIT+"'";
                    sql_1 += ",to_date('"+CREATE_DATE+"','yyyy-mm-dd hh24:mi:ss')";
                    sql_1 += ",'"+CREATE_BY+"'";
                    sql_1 += ")";
                    jdbcTemplate.update(sql_1);
            }

            System.out.println("---------"+date+",同步数据结束---------");
        }catch (Exception e){
            System.out.println("---------同步数据异常---------");
            e.printStackTrace();
        }
    }
}
