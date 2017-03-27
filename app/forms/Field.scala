package forms

import jto.validation.forms.PM
import jto.validation.{ValidationError, Path}
import play.api.i18n.Lang

sealed trait Field {

  def path: Path
  def value: Seq[String]
  def errors: Seq[ValidationError]

  val name: String =
    PM.asKey(path)

  val id: String =
    name.replaceAll("\\.", "-").replaceAll("\\[]", "")

  def hasErrors: Boolean =
    errors.nonEmpty

  def error(implicit lang: Lang): String =
    errors.toMessage

  def rpError(implicit lang: Lang): String =
    errors.headOption.map(_.message)

}

case class ValidField(
                       path: Path,
                       value: Seq[String]
                     ) extends Field {
  override val errors = Seq.empty
}

case class InvalidField(
                         path: Path,
                         value: Seq[String],
                         errors: Seq[ValidationError]
                       ) extends Field
