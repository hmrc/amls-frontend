/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package forms

import jto.validation._
import jto.validation.forms.{PM, UrlFormEncoded}
import jto.validation.ValidationError
import cats.data.Validated.{Invalid, Valid}
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
  override def errors(path: Path): Seq[ValidationError] = Seq.empty

  override def apply(path: Path): Field =
    data.get(PM.asKey(path)).fold[Field](InvalidField(path, Seq.empty, Seq.empty)) {
      v =>
        ValidField(path, v)
    }
}

case class InvalidForm(
                        data: UrlFormEncoded,
                        errors: Seq[(Path, Seq[ValidationError])]
                      ) extends CompletedForm[Nothing] {

  override def errors(path: Path): Seq[ValidationError] =
    errors.toMap.getOrElse(path, Seq.empty)

  override def apply(path: Path): Field = {
    val v = data.getOrElse(PM.asKey(path), Seq.empty)
    val e = errors.toMap.getOrElse(path, Seq.empty)
    InvalidField(path, v, e)
  }
}

object EmptyForm extends Form2[Nothing] {
  override val data: UrlFormEncoded = Map.empty
  override def apply(path: Path): Field = InvalidField(path, Seq.empty, Seq.empty)
  override def errors(path: Path): Seq[ValidationError] = Seq.empty
  override val errors: Seq[(Path, Seq[ValidationError])] = Seq.empty
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
      case Valid(a) => ValidForm(data, a)
      case Invalid(errors) => InvalidForm(data, errors)
    }

  def apply[A]
  (data: AnyContent)
  (implicit rule: Rule[UrlFormEncoded, A]
    ): CompletedForm[A] =
    Form2[A](data.asFormUrlEncoded.getOrElse(Map.empty[String, Seq[String]]): UrlFormEncoded)
}
