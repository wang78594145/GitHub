
object LearnScala {
  def main(args: Array[String]): Unit = {
    //    for(i <-1 to 10){
    //      print(i)
    //    }
    //    println(1)
    //  }
//      for (i <- "hello"){
//        print(i)
//      }
    //println(2)
//        import scala.util.control.Breaks._
//        breakable {
//
//          for (i <- 1 until (10)) {
//            print(i)
//            if(i==8){
//              break()
//            }
//          }
//        }
    import scala.util.control.Breaks._
    breakable{

      for(i <- -1 until( 10)){
        print(i)
        if(i==8){
          break()
        }
      }
    }
//    def abs(x: Double) = if (x >= 0) x else -x
//
//    //根据等号右边的表达式来判断返回值类型
//    def abs2(x: Double) = Double {
//      if (x > 0) {
//        x
//      } else {
//        -x
//
//      }
//    }
//
//    def abs3(n:Int)= {
//      var r = 1;
//      for (i <- 1 to 10) r = r * i // 赋值表达式的结果为（），所以多写一行 r 是该方法的值
//      r
//    }
//    val array=new Array[Int](10) //
//    val arr=Array("hello",true);
//    for(i <-arr){
//      print(i)
//    }
//    for (i <- 0 until(arr.length)){
//      print(arr(i))
//    }
//    println()
//    for (i<- 0 until(arr.length,2)){
//      print(arr(i))
//    }
//    println()
//    for (i <- (0 until(arr.length)).reverse){
//      print(i);print(arr(i))
//    }phoenix-4.7.1-HBase-0.98-SNAPSHOT-client.jar
//    println()
//    for(i<-arr){
//      print(i)
//    }
    val a=Array(1,2,3,4,5,6,7)
//    val b=for(i <- a if(i%2==0)) yield 3*i
    val b=for(i<- a if(i%2==0)) yield 3*i
    print(b.toList)
    var map=Map(("wang",10),("xiao",20),("dong",30))
//    val c=map.get("wang")
    val c= map("wang")
    println(c)
    print(map.getOrElse("xiao",0))
//    println()
//    print(map.getOrElse("ddong",0))
    map+=("liao"->80,"jia"->10)
    map+=(("yi",20),("yiyi",100))
    print(map.toList)
    print((map.values.toList))
//    map += ("liao" -> 80,"jia" -> 90)
//    println("````````````````````````")phoenix-4.7.1-HBase-0.98-SNAPSHOT-client.jar
//    for (k <-map.values){
//      print(k)
//    }phoenix-4.7.1-HBase-0.98-SNAPSHOT-client.jar
//    for ((k,v) <- map){
//      println((k,v))
//    }
    val arrr=Array(1,2,5,3,37,5,49)
    val aSort=arrr.sortWith(_>_).toList //自定义排序顺序返回ArrayBuffer
    val aaSort=arrr.sorted //返回一个ArrayBuffer
    print(aaSort.toList)
    println()
    val aaa=scala.util.Sorting.quickSort(arrr) //直接对数组进行排序,返回Array（），直接改变值的arrr本身
    val bb="woshi,nidie,nizhidao?ma,"
//    bb.mkString(",")  //直接改变值的本身
//    print(bb)
//    println()
    val bbb=bb.mkString("<", ",", ">")     //将元素分割开来并且指定元素之间的分隔符和前缀和后缀
    println(bbb)
    println(bbb.count(x=>x>0))

    val test=List("localhost:2181","lcoalh:2000")
    val test2=test.toString()
    println(test2)

//    print(aSort)
  }

}
