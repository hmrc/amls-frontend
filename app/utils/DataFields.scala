package utils

import play.api.i18n.{Messages, Lang}

object DataFields {

  def optionsYesNo(implicit lang: Lang) = Seq(
    Messages("lbl.yes") -> "true",
    Messages("lbl.no")  -> "false"
  )

  def optionsRegisteredFoMLR(implicit lang: Lang) = Seq(
    Messages("aboutthebusiness.registeredformlr.lbl.yes.withreg") -> "01",
    Messages("aboutthebusiness.registeredformlr.lbl.yes.withpastreg") -> "02",
    Messages("aboutthebusiness.registeredformlr.lbl.no")  -> "03"
  )
}
