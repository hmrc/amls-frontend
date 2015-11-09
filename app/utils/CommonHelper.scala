package utils

import play.api.i18n.Messages

import scala.collection.mutable.ListBuffer

object CommonHelper {
  /**
   * Looks in the messages file for all keys starting with the specified key
   * followed by a dot and an integer. Constructs a Seq of message values in pairs
   * from the keys it finds, starting from integer 1.
   */
  def getSeqFromMessagesKey(key:String, f:String => String): Seq[(String,String)] = {
    var items = new ListBuffer[ (String, String) ]()

    var a = 1
    var continue = true
    while (continue) {
      val currentKey = key + "." + a.toString
      val currentValue = f(currentKey)
      continue = currentValue != currentKey
      if (continue)
        items += ( currentValue -> currentValue )
      a = a + 1
    }
    items.toSeq
  }
}