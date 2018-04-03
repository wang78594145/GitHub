import org.apache.avro.generic.GenericData.StringType
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.StructField

class IndexPra   {


  def pageVist(output:String,sql:String): Unit ={
    //页面浏览量：用户对页面的浏览次数。用page表的id
    val conf=new SparkConf().setMaster("local[4]").setAppName("PV")
    val session=SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    val dataDF=session.read.format("csv").option("header",true).load("/Users/growingio/input")
//    dataDF.printSchema()
    dataDF.createOrReplaceTempView("Page")
    val selectData=session.sql(sql).write.format("csv").option("header",true).save(output)
  }
  def vistor(output:String,sql:String): Unit ={
    //页面用户访问量
    val conf=new SparkConf().setMaster("local[2]").setAppName("vistor")
    val session=SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    val dataDF=session.read.format("csv").option("header",true).load("/Users/growingio/input")
    dataDF.createOrReplaceTempView("Vist")
    val selectData=session.sql("select count(id) from Page").write.format("csv").option("header",true).save(output)


  }
  def newVist(output:String,sql:String): Unit ={
    // 新访问用户量：对网站app有过访问的新用户数量，过去365天内对网站、App没有过访问的用户定义为新用户
    val conf=new SparkConf().setAppName("local[2]").setAppName("newVist")
    val session=SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    val dataDF=session.read.format("csv").option("header",true).load("/Users/growingio/input")//当天的数据
    val dataDF2=session.read.format("csv").option("header",true).load("/Users/growingio/input2")//一年的数据
    dataDF.createOrReplaceTempView("VistDay")
    dataDF2.createOrReplaceTempView("VistYear")
    val selectData=session.sql("select count(id) from VistDay left join VistYear on VistDay.id=VistYear.id where VistYear.id is null")
  }
  def loginUser(output:String,sql:String): Unit ={
    //对网站app在登录状态下有过访问的用户数量：判断是否是登陆状况，从page表中的Cs字段，CS1字段记录的是登陆的用户名信息，如果是登陆状态则CS1会有对应的数据
    val conf=new SparkConf().setMaster("local[2]").setAppName("loginUser")
    val session=SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    val dataDF=session.read.format("csv").option("header",true).load("/Users/growingio/input")
    dataDF.createOrReplaceTempView("Page")
    val selectData=session.sql("select distinct count(userId) where cs1 is not null ")
  }

  def newLoginUser(output:String,sql:String): Unit ={
    //对网站app在登录状态下有过访问的新用户数量
    val conf=new SparkConf().setAppName("newLoginUser").setMaster("local[2]")
    val session=SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    val dataDF=session.read.format("csv").option("header",true).load("/Users/growingio/input")//今天的数据
    dataDF.createGlobalTempView("PageDay")
    val dataDF2=session.read.format("csv").option("header",true).load("/Users/growingio/input2")//一年的数据
    dataDF2.createOrReplaceTempView("PageYear")
    val selectData=session.sql("select count(userId) from PageDay left join PageYear on PageDay.userId=PageYear.userId where PageYear.id is null")
  }
  def vist(output:String,sql:String): Unit ={
    //访问量
    //网站、App的访问的数量。用户从进入网站（打开App）到离开为止的一个相对完整连续的操作过程定义为一个访问。
    // 当访问量被页面级维度分解时，访问量指包含了当前页面级维度的当前元素的页面浏览（一次或多次，至少一次）的访问的次数
    val sql="select count(Vist.id),Vist.domain,... from Vist left join Page on Vist.id=Page.vistId"


  }
  def xxx(): Unit ={
    //index 7访问用户人均访问次数

    //平均每个用户访问网站（打开App）的次数。当该指标被页面级维度分解时，
    // 指浏览过当前页面级维度的当前元素的用户平均访问过当前页面级维度的当前元素的次数
    val sql7="select select count(distinct Vist.id)/count(distinct userId) from Vist)"

    //index 8总访问时长 (分钟)

    //所有访问的总时长，以分钟作为单位展示。不能使用页面级维度分解
    val sql8="select 30*id from Vist"

    //index 9每次访问页面浏览量

    //平均每次访问带来的页面浏览的数量。当该指标被页面级维度分解时，
    // 指有过当前页面级维度的当前元素的页面浏览的那些访问在平均情况下的页面浏览量（
    // 这个页面浏览量不受当前页面级维度的当前元素约束）
    val sql9="select count(id)/count(vist.id) from Page"

     //index 10 进入量

    //访问用户进入网站进行访问的数量。当该指标被页面级维度分解时，指从当前页面级维度的当前元素进入
    //（以当前页面级维度的当前元素作为当前页面级维度的第一个值）的那些访问的数量。
    //在作为指标单独使用或者被除页面级维度以外的维度分解时进入量在数值上等于访问量
    val sql10="select count(vistId),Vist.platform ```` left join Vist on Vist.id=Page.istID"

    //index 11 访问用户人均进入次数

    //平均每个访问用户进入网站进行访问的数量。
    //当该指标被页面级维度分解时，指浏览过当前页面级维度的当前元素的用户平均从当前页面级维度的当前元素进入的次数
    val sql11="select count(VistID)/count(userId) from Page"


    //index 12总进入时长 (分钟)

    //用户进入网站进行访问的总时长，以分钟作为单位展示。当该指标被页面级维度分解时，指从当前页面级维度的当前元素进入
    //（以当前页面级维度的当前元素作为当前页面级维度的第一个值）的那些访问的总时长。
    //在作为指标单独使用或者被除页面级维度以外的维度分解时“总进入时长（分钟）”在数值上等于“总访问量时长（分钟）

    val sql1 = "(select visitId,eventTime from page)T"
    val sql2 = "(select visitId, max(eventTime) - min(eventTime) as tm from T group by visitId)T2"
    val sql3 = "(select sum(tm) from T2) T3 "

    val sql12="select vistId,SUM(time) from ((select distinct vistId,(max(evenTime)-min(evenTime)) as time from (select vistId,id,eventTime from page ) T  groupby sessionID) TEMP2) TEMP3"

    //index 13平均进入时长 (分钟)
    val sql13="select time/count(1) from TEMP3"
    //用户平均每次进入网站进行访问的平均时长，以分钟作为单位展示。当该指标被页面级维度分解时，
    // 指从当前页面级维度的当前元素进入（以当前页面级维度的当前元素作为当前页面级维度的第一个值）的那些访问的平均时长。
    //在作为指标单独使用或者被除页面级维度以外的维度分解时“平均进入时长（分钟）”在数值上等于“平均访问量时长（分钟）

    //index 14跳出次数

    //在对网站的访问过程中，只有一个页面浏览的访问的次数。当该指标被页面级维度分解时，
    //指从当前页面级维度的当前元素进入的那些访问中，只有一个页面浏览的访问的次数
    //val sql14="select count(1) as t1 from (select count(vistId) cnt from Page group by vistId where cnt = 1) T"


    val sql14 = "select sum(t1)/count(1) from (select case when cnt = 1 then 1 else 0 end as t1 from (select visitId, count(vistId) cnt from Page group by vistId) T) TEMP2"

    //index 15跳出率

    //在对网站的访问过程中，只有一个页面浏览的访问占所有访问的比率。
    //当该指标被页面级维度分解时，指从当前页面级维度的当前元素进入访问里面，只有一个页面浏览的访问的比率
    val sql16=""
4
  }

  def practice(): Unit ={
    //index 0:页面浏览量
    val sql0="select count(id) from Page"

    //index 1:访问用户量
    val sql1="select count(userId) from Visit"

    //index 2:新访问用户量
    val sql2="select count(PDU) from ((select PageDay.userId as PDU,PageYear.user as PYU from PageDay left join PageYear on PageDay.userID=PageYear.userId where) T )where T.VYU is null"

    //index 3: 登陆用户量
    val sql3="select count(userID) from Page where cs1 is not null"

    //index 4:新登陆用户量
    val sql4="select  count（userId）from Table（新访问用户量） where cs1 is not null"

    //index 5: 总访问量
    val  sql5="select count(id) from Vist"

    //index 6:   访问用户人均访问次数
    val sql6="select count(id)/count(userID) from Vist"

    //index 7: 访问总时长
    val sql7="select userId,SUM(time) from (select userId,max(evenTime)-min(eventime) as time fromt (select userId,evenTime from from Vist) T group by id) T2"

    //index 8:平均访问时长
    val sql8="select SUM(time)/count(1) from Table(访问总时长)"

    //index 9:每次访问页面浏览量
    val sql9="select count(id)/count(vistId) from Page "

    //index 10:进入量
    "如果不加任何维度同index 0否则加入维度进行条件限制即可"

    //index 11:访问用户人均进入次数
    val sql11="select count(id)/count(userId) FROM Vist ，如果加入维度通过维度进行条件限制比如GroupBy分组等"

    //index 12 总进入时长 (分钟)
    val sql12="select vistId,SUM(time) from ((select distinct vistId,(max(evenTime)-min(evenTime)) as time from (select vistId,id,eventTime from page ) T  groupby sessionID) TEMP2) TEMP3"

    //index 13: 平均进入时长
     val sql13="select time/count(1) from TEMP3"

    //index 14: 每次进入页面浏览量
    val  sql14="select count(id)/count(vistId) FROM Page维度分解是加入限制条件如GroupBY即可"

    //index 15:跳出次数
    val sql15="select SUM(cnt) from (select case count(id) when 1 then 1 else 0 end as cnt from（select vistId,userId,count(id) from Page group by vistId） T) T2"


    //index  16：跳出率 count(*)在只有一个字段的时候比count（1）快
    val sql16="select SUM(cnt)/count(*) from (select case count(id) when 1 then 1 else 0 end as cnt from（select vistId,userId,count(id) from Page group by vistId） T) T2"

    //index 17 :退出次数



  }

  def main(args: Array[String]): Unit = {
    val sql="select count(id) from Page"
    val output="/Users/growingio/output"
    pageVist(output,sql)
    "select substr(trim(ai),length(trim(ai)),-2),count(distinct x) from gio.action  where time='201803190600' and length(trim(ai))>=2 group by substr(trim(ai),length(trim(ai)),-2) limit 10"
  }



" select sum(Amount) ，count(distinct Order ID )   from A where Device ='Mobile'\n-- select sum(Amount) from (select B.UserID as UserID, Month(A.Date) as month, B.City as City ,B.Gender as Gender A.Amount as Amount from B inner join A on B.UserID=A.UserID)T where T.City='Shanghai' and Gender='Male' group by T.month\n--  select Date, Device as Device Combo ,count(UserID) as Total Buyer, count(OrderID) as TotaOrder,sum(Amount) as TotalAmount from A group by Date \n-- select T.Date as Date,T.Device as DeviceComb,count(T.UserID) as TotalBuyer ,count(OrderID) as TotaOrder,sum(Amount) as TotalAmount from (select C.Date as Date ,A.Device as Device Combo ,A.UserID as UserID, A.OrderID as OrderID,A.Amount as Amount from C inner join A on C.MonthBeginDate=A.Date) T group by T.Date\nselect T3.WeeklyBeginDate as WeeklyBeginDate,T3.Country as Country,T3.DeviceCombp as DeviceCombp,count(T3.UserID) as TotalBuyer,count(T3.OrderID) as TotalOrder,sum(T3.Amount) as Amount from (select C.WeeklyBeginDate as WeeklyBeginDate ,T2.Country as Country,T2.DeviceCombo as DeviceCombo ,T2.UserID as UserID, T2.OrderID as OrderID,T2.Amount as Amount  from (select A.Device as DeviceCombo,A.UserID as UserID,A.OrderID as OrderID,A.Amount as Amount,B.Country as Country from B INNER JOIN A ON A.UserID=B.UserID) T2) T3 group by WeeklyBeginDate "
//  select sum(Amount) ，count(distinct Order ID )   from A where Device ='Mobile'
//  -- select sum(Amount) from (select B.UserID as UserID, Month(A.Date) as month, B.City as City ,B.Gender as Gender A.Amount as Amount from B inner join A on B.UserID=A.UserID)T where T.City='Shanghai' and Gender='Male' group by T.month
//  --  select Date, Device as Device Combo ,count(UserID) as Total Buyer, count(OrderID) as TotaOrder,sum(Amount) as TotalAmount from A group by Date
//    -- select T.Date as Date,T.Device as DeviceComb,count(T.UserID) as TotalBuyer ,count(OrderID) as TotaOrder,sum(Amount) as TotalAmount from (select C.Date as Date ,A.Device as Device Combo ,A.UserID as UserID, A.OrderID as OrderID,A.Amount as Amount from C inner join A on C.MonthBeginDate=A.Date) T group by T.Date
//  select T3.WeeklyBeginDate as WeeklyBeginDate,T3.Country as Country,T3.DeviceCombp as DeviceCombp,count(T3.UserID) as TotalBuyer,count(T3.OrderID) as TotalOrder,sum(T3.Amount) as Amount from (select C.WeeklyBeginDate as WeeklyBeginDate ,T2.Country as Country,T2.DeviceCombo as DeviceCombo ,T2.UserID as UserID, T2.OrderID as OrderID,T2.Amount as Amount  from (select A.Device as DeviceCombo,A.UserID as UserID,A.OrderID as OrderID,A.Amount as Amount,B.Country as Country from B INNER JOIN A ON A.UserID=B.UserID) T2) T3 group by WeeklyBeginDate
//


}
