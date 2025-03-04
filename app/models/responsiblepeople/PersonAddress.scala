/*
 * Copyright 2024 HM Revenue & Customs
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

import models.Country
import play.api.i18n.{Messages, MessagesProvider}
import play.api.libs.json.{Reads, Writes}

sealed trait PersonAddress {

  def toLines: Seq[String] = this match {
    case a: PersonAddressUK    =>
      Seq(
        Some(a.addressLine1),
        a.addressLine2,
        a.addressLine3,
        a.addressLine4,
        Some(a.postCode)
      ).flatten
    case a: PersonAddressNonUK =>
      Seq(
        Some(a.addressLineNonUK1),
        a.addressLineNonUK2,
        a.addressLineNonUK3,
        a.addressLineNonUK4,
        Some(a.country.toString)
      ).flatten
  }

  def isUK()(implicit provider: MessagesProvider): String = this match {
    case _: PersonAddressUK    => Messages("lbl.yes")
    case _: PersonAddressNonUK => Messages("lbl.no")
  }

  def isComplete = this match {
    case PersonAddressUK(al1, _, _, _, ap) if al1.nonEmpty & ap.nonEmpty                         => true
    case PersonAddressNonUK(al1, _, _, _, c) if al1.nonEmpty & c.name.nonEmpty & c.code.nonEmpty => true
    case _                                                                                       => false
  }

  def isEmpty = this match {
    case PersonAddressUK("", _, _, _, "")                 => true
    case PersonAddressNonUK("", _, _, _, Country("", "")) => true
    case _                                                => false
  }
}

case class PersonAddressUK(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  postCode: String
) extends PersonAddress

case class PersonAddressNonUK(
  addressLineNonUK1: String,
  addressLineNonUK2: Option[String],
  addressLineNonUK3: Option[String],
  addressLineNonUK4: Option[String],
  country: Country
) extends PersonAddress

object PersonAddress {

  implicit val jsonReads: Reads[PersonAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (__ \ "personAddressPostCode").read[String] andKeep (
      ((__ \ "personAddressLine1").read[String] and
        (__ \ "personAddressLine2").readNullable[String] and
        (__ \ "personAddressLine3").readNullable[String] and
        (__ \ "personAddressLine4").readNullable[String] and
        (__ \ "personAddressPostCode").read[String])(PersonAddressUK.apply _) map identity[PersonAddress]
    ) orElse
      ((__ \ "personAddressLine1").read[String] and
        (__ \ "personAddressLine2").readNullable[String] and
        (__ \ "personAddressLine3").readNullable[String] and
        (__ \ "personAddressLine4").readNullable[String] and
        (__ \ "personAddressCountry").read[Country])(PersonAddressNonUK.apply _)
  }

  implicit val jsonWrites: Writes[PersonAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[PersonAddress] {
      case a: PersonAddressUK    =>
        (
          (__ \ "personAddressLine1").write[String] and
            (__ \ "personAddressLine2").writeNullable[String] and
            (__ \ "personAddressLine3").writeNullable[String] and
            (__ \ "personAddressLine4").writeNullable[String] and
            (__ \ "personAddressPostCode").write[String]
        )(unlift(PersonAddressUK.unapply)).writes(a)
      case a: PersonAddressNonUK =>
        (
          (__ \ "personAddressLine1").write[String] and
            (__ \ "personAddressLine2").writeNullable[String] and
            (__ \ "personAddressLine3").writeNullable[String] and
            (__ \ "personAddressLine4").writeNullable[String] and
            (__ \ "personAddressCountry").write[Country]
        )(unlift(PersonAddressNonUK.unapply)).writes(a)
    }
  }
}
