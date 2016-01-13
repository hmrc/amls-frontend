package forms

import play.api.data.mapping
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.mvc.AnyContent

sealed trait Form2[+A] {

  def data: UrlFormEncoded

  /** Should this return Option[Seq[String]]?
    * Would that be easy enough to work with? */
  def apply(path: Path): Option[String] =
    data.get(path.toString.substring(1)) map { _.mkString("") }

  def apply(path: String): Option[String] =
    data.get(path) map { _.mkString("") }

  def errors: Seq[(Path, Seq[ValidationError])]
  def errors(path: Path): Seq[ValidationError]
}

sealed trait CompletedForm[+A] extends Form2[A]

case class ValidForm[A](data: UrlFormEncoded, model: A) extends CompletedForm[A] {
  override def errors: Seq[(Path, Seq[ValidationError])] = Seq.empty
  override def errors(path: Path): Seq[mapping.ValidationError] = Seq.empty
}

case class InvalidForm(data: UrlFormEncoded, errors: Seq[(Path, Seq[ValidationError])]) extends CompletedForm[Nothing] {
  override def errors(path: Path): Seq[ValidationError] =
    errors.collectFirst {
      case (p, e) if (p == path) => e
    } getOrElse Seq.empty
}

case object EmptyForm extends Form2[Nothing] {
  override def data: UrlFormEncoded = Map.empty
  override def errors(path: Path): Seq[mapping.ValidationError] = Seq.empty
  override def errors: Seq[(Path, Seq[mapping.ValidationError])] = Seq.empty
}

object Form2 {

  def apply[A]
  (a: A)
  (implicit
   write: Write[A, UrlFormEncoded]
    ): ValidForm[A] =
    ValidForm(write.writes(a), a)

  def apply[A]
  (data: UrlFormEncoded)
  (implicit
   rule: Rule[UrlFormEncoded, A]
    ): CompletedForm[A] =
    rule.validate(data) match {
      case Success(a) => ValidForm(data, a)
      case Failure(errors) => InvalidForm(data, errors)
    }

  def apply[A]
  (data: AnyContent)
  (implicit rule: Rule[UrlFormEncoded, A]
    ): CompletedForm[A] =
    Form2[A](data.asFormUrlEncoded.getOrElse(Map.empty[String, Seq[String]]): UrlFormEncoded)
}