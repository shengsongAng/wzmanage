package com.jysoft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

//    @RequestMapping("/index")
////    public String index(){
////        return "index";
////    }

@RequestMapping("/wzmanage")
public String wzmanage(HttpServletRequest request, @RequestParam(value="usernumber",required=false) String usernumber){
    HttpSession session = request.getSession();

    if(usernumber==null){
        usernumber="";
    }
    session.setAttribute("usernumber",usernumber);
    return "wzmanage";
}

    @RequestMapping("/index")
    public String index(HttpServletRequest request){
        reSaveSession(request);
        return "index";
    }

    @RequestMapping("/uploadExcel")
    public String uploadExcel(){
        return "uploadExcel";
    }

    @RequestMapping("/multiUse")
    public String multiUse(){
        return "multiUse";
    }

    @RequestMapping("/storageDetailList")
    public String storageDetailList(HttpServletRequest request){
        reSaveSession(request);
        return "storageDetailList";
    }

    @RequestMapping("/useDetailList")
    public String useDetailList(HttpServletRequest request){
        reSaveSession(request);
        return "useDetailList";
    }

    @RequestMapping("/monthStatisticList")
    public String monthStatisticList(HttpServletRequest request){
        reSaveSession(request);
        return "monthStatisticList";
    }

//    @RequestMapping("/detailByWz")
//    public String detailByWz(Model model, @RequestParam("desc_wz") String desc_wz){
//        model.addAttribute("name",java.net.URLDecoder.decode(desc_wz));
//        return "detailByWz";
//    }
    @RequestMapping("/detailByWz")
    public String detailByWz(){
        return "detailByWz";
    }

//    @RequestMapping("/showStorageDetail")
//    public String showStorageDetail(Model model, @RequestParam("desc_wz") String desc_wz){
//        model.addAttribute("name",java.net.URLDecoder.decode(desc_wz));
//        return "showStorageDetail";
//    }

    @RequestMapping("/showStorageDetail")
    public String showStorageDetail(){
        return "showStorageDetail";
    }

//    @RequestMapping("/showUseDetail")
//    public String showUseDetail(Model model, @RequestParam("desc_wz") String desc_wz){
//        model.addAttribute("name",java.net.URLDecoder.decode(desc_wz));
//        return "showUseDetail";
//    }

    @RequestMapping("/showUseDetail")
    public String showUseDetail(){
        return "showUseDetail";
    }


    //重新获取session，并保存，防止session过期
    public void reSaveSession(HttpServletRequest request){
        HttpSession session = request.getSession();
        String usernumber = session.getAttribute("usernumber").toString();
        if(!StringUtils.isEmpty(usernumber)){
            session.setAttribute("usernumber",usernumber);
        }
    }
}
