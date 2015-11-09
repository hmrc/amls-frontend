package utils

import play.api.i18n.Messages

import scala.collection.mutable.ListBuffer

object CommonHelper {
  def getSeqFromMessagesKey(label:String): Seq[(String,String)] = {
    var items = new ListBuffer[ (String, String) ]()

    var a = 1
    var continue = true
    while (continue) {
      val currentKey = label + "." + a.toString
      val currentValue = Messages(currentKey)
      continue = currentValue != currentKey
      if (continue)
        items += ( currentValue -> currentValue )
      a = a + 1
    }
    items.toSeq
  }
}