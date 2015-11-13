package utils

import scala.annotation.tailrec

object CommonHelper {
  @tailrec
  def getSeqFromMessagesKey(key:String,
                            messageLookupFunction:String => String,
                            startValue:Int = 1,
                            items:Seq[(String, String)]  = Nil ): Seq[(String, String)]  = {
    val currentKey = key + "." + startValue.toString
    val currentValue = messageLookupFunction(currentKey)
    if (currentValue != currentKey) {
      getSeqFromMessagesKey(key, messageLookupFunction, startValue + 1, items :+ ( currentValue -> currentValue ))
    } else {
      items
    }
  }

  def mapSeqWithMessagesKey(seq:Seq[String],
                            key: String,
                             messageLookupFunction:String => String):Seq[(String,String)] = {
    for(e<-seq) yield {
      val currentKey = key + "." + e
      val currentValue = messageLookupFunction(currentKey)
      (if (currentValue == currentKey) "" else currentValue) -> e
    }
  }

}

