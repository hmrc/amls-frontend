package forms

import play.api.data.mapping
import play.api.data.mapping._
import play.api.data.mapping.forms.{PM, UrlFormEncoded}
import play.api.data.validation.ValidationError
import play.api.mvc.AnyContent

sealed trait Form2[+A] {

  def data: UrlFormEncoded

  def apply(path: Path): Field
  def errors: Seq[(Path, Seq[ValidationError])]
  def errors(path: Path): Seq[ValidationError]

  def apply(path: String): Field =
    apply(PM.asPath(path))
}

sealed trait CompletedForm[+A] extends Form2[A]

case class ValidForm[A](data: UrlFormEncoded, model: A) extends CompletedForm[A] {

  override def errors: Seq[(Path, Seq[ValidationError])] = Seq.empty
  override def errors(path: Path): Seq[mapping.ValidationError] = Seq.empty

  private def collapse(seq: Seq[String]) = seq.mkString("")

  override def apply(path: Path): Field =
    data.get(PM.asKey(path)).fold[Field](InvalidField(path, None, Seq.empty)) {
      v =>
        ValidField(path, Some(collapse(v)))
    }
}

case class InvalidForm(
                        data: UrlFormEncoded,
                        errors: Seq[(Path, Seq[ValidationError])]
                      ) extends CompletedForm[Nothing] {

  override def errors(path: Path): Seq[ValidationError] =
    errors.toMap.get(path).getOrElse(Seq.empty)

  override def apply(path: Path): Field = {
    val v = data.get(PM.asKey(path)) map { _.mkString("") }
    val e = errors.toMap.get(path).getOrElse(Seq.empty)
    InvalidField(path, v, e)
  }
}

case object EmptyForm extends Form2[Nothing] {
  override val data: UrlFormEncoded = Map.empty
  override def apply(path: Path): Field = InvalidField(path, None, Seq.empty)
  override def errors(path: Path): Seq[mapping.ValidationError] = Seq.empty
  override val errors: Seq[(Path, Seq[mapping.ValidationError])] = Seq.empty
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