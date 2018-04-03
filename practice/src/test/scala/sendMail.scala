
import java.util.{Date, Properties}

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{SQLContext, SparkSession}
import java.io._
import java.net.URI
import java.text.SimpleDateFormat
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeUtility

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, FileUtil, Path}
import org.apache.hadoop.io.IOUtils


class SendMail {

  private var sql: String = ""
  private var hdfsFileName: String = ""

  private var from: String = " "
  private var fromPassword: String = ""
  private var localFileName: String = ""
  private val to: String = ""
  private var fileName = ""
  //  private val now = new Date()
  //  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss")
  //  private val time = dateFormat.format(now)


  /**
    * 生成sql查询 的DF，将其保存在hdfs中
    *
    * @param subject 主题
    * @param sql     sql语句
    */
  private def saveToHDFS(subject: String, sql: String, time: String): Unit = {

    //文件名
    fileName = subject + time
    //文件在hdfs的绝对路径
    val hdfsFileName = "/Users/growingio/apps/zeppelin-0.7.4-SNAPSHOT/logs/" + fileName
    val conf = new SparkConf().setMaster("local[4]").setAppName("MyPra")
    val spark = SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    val df = spark.sql(sql).persist()
    df.repartition(1).write.format("csv").option("header", "true").save(hdfsFileName)

  }

  /**
    * 将查询结果，发送给指定的人：邮件发送
    *
    * @param from         ：发件人邮箱
    * @param fromPassword ：发件人邮箱密码
    * @param subject      ：发件主题
    *
    *
    */

  private def sendEmai(from: String, fromPassword: String, subject: String, recipients: String): Unit = {


    // 发件人电子邮箱
    //
    // 指定发送邮件的主机为 smtp.qq.com
    val host = "smtp.qiye.163.com"
    //163企业 邮件服务器
    // 获取系统属性
    val properties: Properties = new Properties()
    // 设置邮件服务器,主机地址
    properties.setProperty("mail.smtp.host", host)
    //设置邮件协议
    properties.setProperty("mail.transport.protocol", "smtp");
    // 认证
    properties.setProperty("mail.smtp.auth", "true")
    // 端口
    properties.setProperty("mail.smtp.port", "25")

    properties.setProperty("mail.smtp.starttls.enable", "true")

    // val sf = new MailSSLSocketFactory
    // sf.setTrustAllHosts(true)
    // properties.put("mail.smtp.ssl.enable", "true")
    // properties.put("mail.smtp.ssl.socketFactory", sf)
    //2: 创建session，获取默认session对象
    val session = Session.getInstance(properties, new Authenticator() {
      override def getPasswordAuthentication: PasswordAuthentication = {
        //qq邮箱服务器账户、第三方登录授权码
        return new PasswordAuthentication(from, fromPassword) //发件人邮件用户名、密码

      }
    })
    //开启debug模式，这样就可以查看到程序发送Email的运行状态
    session.setDebug(true)

    //创建邮件
    //创建邮件对象
    val message = new MimeMessage(session)
    // Set Subject: 设置邮件标题
    message.setSubject(subject)
    //邮件发送日期
    message.setSentDate(new Date())
    // message.setText("Dear Mail Crawler," + "\n\n No spam to my email, please!")
    // Set From: 头部头字段 指明邮件的发件人
    message.setFrom(new InternetAddress(from))
    // Set To: 头部头字段,指明收件人
    //        message.setRecipient(Message.RecipientType.TO, InternetAddress.parse(to))
    message.setRecipient(Message.RecipientType.TO, new InternetAddress(to))


    // 向multipart对象中添加邮件的各个部分内容，包括文本内容和附件
    // 创建多重消息
    val multipart = new MimeMultipart()


    // 创建消息部分，正文部分
    var contentBodyPart = new MimeBodyPart()
    // 消息
    // contentBodyPart.setText("UserAction")
    // 设置文本消息部分
    val result = "UserActionResult"
    contentBodyPart.setContent(result, "text/html;charset=UTF-8")
    multipart.addBodyPart(contentBodyPart)




    // 附件部分
    if (localFileName != null && !"".equals(localFileName)) {


      val attachmentBodyPart = new MimeBodyPart();
      // 根据附件路径获取文件,
      val dataSource = new FileDataSource(localFileName);
      attachmentBodyPart.setDataHandler(new DataHandler(dataSource));
      //MimeUtility.encodeWord可以避免文件名乱码
      attachmentBodyPart.setFileName(MimeUtility.encodeWord(dataSource.getFile().getName()));
      multipart.addBodyPart(attachmentBodyPart);
    }
    // 邮件的文本内容
    message.setContent(multipart);

    // 4. 发送邮件,Transport每次发送成功程序帮忙关闭
    Transport.send(message, message.getAllRecipients())


  }

  /**
    * 从hdfs中拉取数据到本地
    *
    *
    */
  private def copyFileFromHDFS(subject: String, time: String): Unit = {

    fileName = subject + time
    localFileName = "/Users/growingio/apps/zeppelin-0.7.4-SNAPSHOT/logs" + fileName + ".csv"
    val fs = FileSystem.get(new URI("hdfs://localhost:9000"), new Configuration())
    val files = fs.listStatus(new Path(hdfsFileName))
    val listFileNames = FileUtil.stat2Paths(files)
    val out = new FileOutputStream(localFileName)
    for (l <- listFileNames) {
      if (l.getName.endsWith(".csv")) {
        val fileName = hdfsFileName + "/" + l.getName
        val in = fs.open(new Path(fileName))
        IOUtils.copyBytes(in, out, 4096, true)


      }

    }



    //    val out =new FileOutputStream(localFileName)

    /**
      * IOUtils对io的一个封装
      * BufferedReader封装一个InputStream，默认private val defaultCharBufferSize: Int = 8192，数据缓存大小为8192个字符
      * 4096是对buffersize进行调整
      * 第四个参数，是否关闭流
      *
      */
    //    IOUtils.copyBytes(in, out, 4096, true)

  }

  private def fillterCSV(localPath: String, output: String): Unit = {
    val dir = new File(localPath)
    //过滤出以csv结尾的文件，返回File[]
    val filenames = dir.listFiles(new FilenameFilter() {
      override def accept(dir: File, name: String): Boolean = return name.endsWith(".csv")

    })
  }

  def mergeFile(inputLocalPath: String): Unit = {
    val outputfileName = inputLocalPath + "/" + inputLocalPath + ".csv" //wangtest/wangtest.csv
    val conf = new SparkConf().setMaster("local[4]").setAppName("MyPra")
    val spark = SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    // 采用通配符的模式匹配
    val fileDF = spark.read.format("csv").option("header", true).load(inputLocalPath + "/*.csv")
    fileDF.coalesce(1).write.format("csv").option("header", true).save(outputfileName)


  }


  //    def sendMail(subject: String, recipients: String, sql: String): Unit ={
  //
  //      saveToHDFS(subject, sql)
  //      copyFileFromHDFS()
  //      sendEmai("wangxiaodong@growingio.com", "BABY7758521!", subject,  recipients)
  //
  //
  //    }
  def main(args: Array[String]): Unit = {
    val localFileName = "/Users/growingio/tmp/WangXiaodongTest201803280949024.csv"
    val fs = FileSystem.get(new URI("hdfs://localhost:9000"), new Configuration())
    val in = fs.open(new Path("hdfs://localhost:9000/tmp/WangXiaodongTest201803280949024.csv"))
    val out = new FileOutputStream(new File(localFileName), true)

    /**
      * IOUtils对io的一个封装
      * BufferedReader封装一个InputStream，默认private val defaultCharBufferSize: Int = 8192，数据缓存大小为8192个字符
      * 4096是对buffersize进行调整
      * 第四个参数，是否关闭流
      *
      */
    IOUtils.copyBytes(in, out, 4096, true)
  }

  def readPhoenix(table: String, server: String): Unit = {
    val sconf = new SparkConf().setMaster("local[4]").setAppName("wang")
    val sc = new SparkContext(sconf)
    val session = SparkSession.builder().config(sconf).enableHiveSupport().getOrCreate()
    val view = session.read.format("").options(Map("table" -> "", "zookeeperURL" -> "")).load().registerTempTable(table)


  }

  def connectHbaseByPhoenix(table: String, hostPort:List[String]):Unit={
    val sc = new SparkContext("local", "phoenix-test")
    val sqlContext = new SQLContext(sc)
    val df = sqlContext.read.format("org.apache.phoenix.spark").options(Map("table" -> table, "zkUrl" ->hostPort.toString())).load.registerTempTable(table)
    sqlContext.sql("").write.format("csv").option("header","true").save("")


  }





  }


    