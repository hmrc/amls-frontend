/*
 * Copyright 2019 HM Revenue & Customs
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

package models.responsiblepeople

import cats.data.Validated.{Invalid, Valid}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{ValidationError, _}
import models.tcsp.ServicesOfAnotherTCSP
import play.api.libs.json.{Json, Reads, Writes}
import utils.MappingUtils.Implicits._

case class PreviousName(
                         hasPreviousName: Option[Boolean] = None,
                         firstName: Option[String],
                         middleName: Option[String],
                         lastName: Option[String]
                       ) {

  val fullName = Seq(firstName, middleName, lastName).flatten[String].mkString(" ")

}

object PreviousName {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._


  implicit val formR: Rule[UrlFormEncoded, PreviousName] = From[UrlFormEncoded] { __ =>

    import jto.validation.forms.Rules._

    type I = (Option[String], Option[String], Option[String])

    val iR = Rule[I, I] {
      case names@(first, middle, last) if names.productIterator.collectFirst {
        case Some(_) => true
      }.isDefined =>
        Valid(names)
      case _ =>
        Invalid(Seq(Path -> Seq(ValidationError("error.rp.previous.invalid"))))
    }

    (__ \ "hasPreviousName").read[Option[Boolean]] flatMap {
      case Some(true) => (
        (
          (__ \ "firstName").read(optionR(genericNameRule())) ~
            (__ \ "middleName").read(optionR(genericNameRule())) ~
            (__ \ "lastName").read(optionR(genericNameRule()))
          ).tupled andThen iR).map(t => PreviousName(Some(true), t._1, t._2, t._3))
      case _ => Rule.fromMapping { _ => Valid(PreviousName(Some(false), None, None, None)) }
    }
  }

  implicit val formWrite = Write[PreviousName, UrlFormEncoded] {
    model =>
      model.hasPreviousName match {
        case Some(true) =>
          Map(
            "hasPreviousName" -> Seq("true"),
            "firstName" -> Seq(model.firstName getOrElse ""),
            "middleName" -> Seq(model.middleName getOrElse ""),
            "lastName" -> Seq(model.lastName getOrElse "")
          )
        case _ =>
          Map("hasPreviousName" -> Seq("false"))
      }
  }

  implicit val jsonReads : Reads[PreviousName] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (__ \ "hasPreviousName").readNullable[Boolean] and
      (__ \ "firstName").readNullable[String] and
      (__ \ "middleName").readNullable[String] and
      (__ \ "lastName").readNullable[String]
  }.apply(PreviousName.apply _)

  implicit val jsonWrites = Writes[PreviousName] { pn =>
    Json.obj("hasPreviousName" -> pn.hasPreviousName,
      "firstName" -> pn.firstName,
      "middleName" -> pn.middleName,
      "lastName" -> pn.lastName
    )
  }
}
