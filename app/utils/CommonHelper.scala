package utils

import play.api.i18n.{Messages, Lang}

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

  def optionsYesNo(implicit lang: Lang) = Seq(
    Messages("lbl.yes") -> "true",
    Messages("lbl.no")  -> "false"
  )

  def optionsRegisteredFoMLR(implicit lang: Lang) = Seq(
    Messages("lbl.hasMLR.yes.withReg") -> "01",
    Messages("lbl.hasMLR.yes.withPastReg") -> "02",
    Messages("lbl.hasMLR.no")  -> "03"
  )

}

