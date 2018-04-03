import javax.print.DocFlavor.STRING

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.FieldAccessor_Float

class SQL_Pra {
  def xx(): Unit ={
    val join="select tableA_name.column(s),tableB.column(s) from tableA left join tableB on tableA.column=tableB.column"
    val inser="insert into table_name(column_name[s]) values(column_context[s])"
    val update="update table_name set(column_name=newcontext )where column=oldcontext"
    val delete="delete * from table_name where column=context"
    val top="select top number column_name[s] from table_name or select top"
    val xx="select case（column_name as int）from table_name"
    /*create table employ（
    name  STRING,
    salary FLOAT,
    subordinates ARRAY<STRING>,
    deductions MAP<STRING,FLOAT>,
    address STRUCT<stree：String，city：String，state：String，zip：INT>
    row farmat delimited
    fields terminated by "\001" 字段划分
    collection items terminated by "\002"
    map keys terminated by "\003"
    lines terminated by "\n"
    stored as textfile;
    */

    /*创建数据库
       create  database if not exists  database-name
       comment '。。。。'
       location '。。。。'hdfs：//localhost：000 /output/。。。  file://output/...
       dbproperties ' xx=dd ，mm=aa'默认会创建两个属性，一个最后修改的用户名db_，一个最后修改的时间
       s删除数据库不可以直接删除，必须要先把数据库中的所有目录删掉之后即表删掉之后才可以删掉这个数据库
       不过可以用 DROP DATABASE db_name CASCADE;会自动的删除表 然后再删除数据库


       show databases
       show databases leke 'h.*';支持正则查找
       describle database db_name
       describle datebase extended mydb；查看详细描述
       */

    /*创建表
    * create table tbl_name if not exists db_name.tbl_name( 可以在表名之前用db_name.tbl.name来指明该表在那个数据库中
    * name STRING COMENT '..the description of the column..'
    * age string comment '....'
    * xxx ARRAY<STRING> comment '...'
    * ddd MAP<STIRNG,DOUBLE> comment '....'
    * nnn STRUCT<stree:string,city:string,state:string,doornumber:int>
    * comment '....the description of table..'
    * tblproperties ('creator'='wang','created_at'='2012-01-02 10:00:00)默认会添加两个属性，1：last_modified_by( 最后修改的用户名) 2：last_modified_time
    * location '....';
    *
    * 拷贝一张表
    * create table if not exists mydb.employee2
    * LIKE mydb.employees
    * location ' ';可以接受lecation 的设置，但是它的属性模式都不可以从新定义
    *
    * show tables in mydb;
    * describle extended mydb.employees;查看表的详细描述
    * describle formatted mydb。employees；可以查看更多详细描述，实际上用formatted更多
    * describle mydb。employees。salary；查看表的某一列的信息
    * )*/
    /*
    * 加载数据
    * LOAD DATA LOCAL INPATH ' .... ' OVERWRITE INTO TABLE employee  PARTITON(CONTRY='US','STATE='AS')
     * 通过查询语句向表中插入数据：INSET OVERWRITE TABLE employee PARTITION(COUNTRY='US',STATE='AS')
     *                         SELECT * FROM stage_employee se
     *                         WHERE se.cnty='US'and se.st='AS'
     *通过查询语句向表中插入多条分区数据：FROM stage_employee se
     *                               INSERT OVERWRITE TABLE employee
     *                                 PARTION(COUNTRY='US',STATE='AS)
     *                                 SELECT * WHERE se.cnty='US' AND se.STATE='AS'
     *                               INSERT OVERWRITE TABLE employee
     *                                 PARTITON (COUNTRY='US',STATE='BS')
     *                                 SELECT * WHERE se.cnty='US' AND se.STATE='BS'
     *                               INSERT OVERWRITE TABNLE employee
     *                                 PARTITON （COUNTRY='US',STATE='CS'）
     *                                 SELECT * WHERE se.cnty='US' AND se.STATE='CS'
     *                  每条数据都会从select where语句 进行判断，这些语句都是独立进行判断的，这不是 if。。then 。。else。。
     *                                */




  }

}
