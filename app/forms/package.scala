import play.api.data.mapping.forms.PM
import play.api.data.mapping.{ValidationError, Path}
import play.api.i18n.{Lang, Messages}

package object forms {

  implicit def optStr(s: Option[String]): String =
    s.getOrElse("")

  implicit class richError(e: ValidationError) {

    def toMessage(implicit lang: Lang): String =
      Messages(e.message, e.args: _*)
  }

  implicit class richErrorList(seq: Seq[ValidationError]) {

    def toMessage(implicit lang: Lang): String =
      seq.headOption map { _.toMessage }
  }

  implicit class richPath(p: Path) {
    def key = PM.asKey(p)
  }
}