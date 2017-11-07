/*
 * Copyright 2017 HM Revenue & Customs
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
import jto.validation.forms.Rules._
import jto.validation.forms._
import models.FormTypes._
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._

case class PersonName(
                       firstName: String,
                       middleName: Option[String],
                       lastName: String
                     ) {

  val fullName = Seq(Some(firstName), middleName, Some(lastName)).flatten[String].mkString(" ")
  val titleName = Seq(Some(firstName), Some(lastName)).flatten[String].mkString(" ")
  val fullNameWithoutSpace = Seq(Some(firstName), middleName, Some(lastName)).flatten[String].mkString("")

}

object PersonName {

  import play.api.libs.json._

  implicit val formRule: Rule[UrlFormEncoded, PersonName] = From[UrlFormEncoded] { __ =>

    (
      (__ \ "firstName").read(genericNameRule("error.required.rp.first_name")) ~
        (__ \ "middleName").read(optionR(genericNameRule())) ~
        (__ \ "lastName").read(genericNameRule("error.required.rp.last_name"))
      )(PersonName.apply _)
  }

  implicit val formWrite = Write[PersonName, UrlFormEncoded] {
    model =>

      Map(
        "firstName" -> Seq(model.firstName),
        "middleName" -> Seq(model.middleName getOrElse ""),
        "lastName" -> Seq(model.lastName)
      )
  }

  implicit val format = Json.format[PersonName]
}
