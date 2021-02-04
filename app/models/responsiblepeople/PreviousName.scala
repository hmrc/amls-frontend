/*
 * Copyright 2021 HM Revenue & Customs
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

import cats.data.Validated.Valid
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.{Json, Reads}

case class PreviousName(hasPreviousName: Option[Boolean] = None,
                        firstName: Option[String],
                        middleName: Option[String],
                        lastName: Option[String]) {

  val fullName = Seq(firstName, middleName, lastName).flatten[String].mkString(" ")
}

object PreviousName {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._

  implicit val formR: Rule[UrlFormEncoded, PreviousName] = From[UrlFormEncoded] { __ =>

    import jto.validation.forms.Rules._

    val firstNameRule = genericNameRule("error.rp.previous.first.invalid",
      "error.rp.previous.first.length.invalid",
      "error.rp.previous.first.char.invalid")

    val middleNameRule = genericNameRule("",
      "error.rp.previous.middle.length.invalid",
      "error.rp.previous.middle.char.invalid")

    val lastNameRule = genericNameRule("error.rp.previous.last.invalid",
      "error.rp.previous.last.length.invalid",
      "error.rp.previous.last.char.invalid")

    (__ \ "hasPreviousName").read[Boolean].withMessage("error.required.rp.hasPreviousName") flatMap {
      case true => (
        (__ \ "firstName").read(firstNameRule) ~
          (__ \ "middleName").read(optionR(middleNameRule)) ~
          (__ \ "lastName").read(lastNameRule)
        ).tupled.map(name => PreviousName(Some(true), Some(name._1), name._2, Some(name._3)))
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

  implicit val jsonWrites = Json.writes[PreviousName]
}
