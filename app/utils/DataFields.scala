package utils

import play.api.i18n.{Messages, Lang}

object DataFields {

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
