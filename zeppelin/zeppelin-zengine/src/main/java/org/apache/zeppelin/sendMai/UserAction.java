package org.apache.zeppelin.sendMai;

import org.apache.zeppelin.notebook.Paragraph;

import java.util.Date;
import java.util.LinkedHashMap;

public class UserAction {
    public String date_User_Sql(Paragraph p){
        Date time=p.getDateStarted();
        String user=p.getUser();
        String scriptBody=p.getScriptBody().replace("\n","\t");
        String text="{" + time + ":" + user + scriptBody + "}";
        LinkedHashMap map=new LinkedHashMap();
        map.put(time,null);

        return null;

    }
}
