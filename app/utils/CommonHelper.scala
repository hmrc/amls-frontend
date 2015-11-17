package utils

object CommonHelper {

  def mapSeqWithMessagesKey(seq: Seq[String],
                            key: String,
                            messageLookupFunction: String => String): Seq[(String, String)] = {
    for (e <- seq) yield {
      val currentKey = key + "." + e
      val currentValue = messageLookupFunction(currentKey)
      (if (currentValue == currentKey) "" else currentValue) -> e
    }
  }

}

