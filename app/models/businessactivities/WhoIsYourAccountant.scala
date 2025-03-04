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

package models.businessactivities

import play.api.libs.json._

case class WhoIsYourAccountant(
  names: Option[WhoIsYourAccountantName],
  isUk: Option[WhoIsYourAccountantIsUk],
  address: Option[AccountantsAddress]
) {
  def names(newName: WhoIsYourAccountantName): WhoIsYourAccountant         = this.copy(names = Option(newName))
  def isUk(newIsUk: WhoIsYourAccountantIsUk): WhoIsYourAccountant          = this.copy(isUk = Option(newIsUk))
  def address(newAddress: AccountantsAddress): WhoIsYourAccountant         = this.copy(address = Option(newAddress))
  def address(newAddress: Option[AccountantsAddress]): WhoIsYourAccountant = this.copy(address = newAddress)

  def isComplete: Boolean = this match {
    case WhoIsYourAccountant(Some(_), Some(_), Some(a)) if a.isComplete => true
    case _                                                              => false
  }
}

object WhoIsYourAccountant {
  val key = "who-is-your-accountant"

  import play.api.libs.functional.syntax._

  implicit val jsonReads: Reads[WhoIsYourAccountant] = (
    __.read(Reads.optionNoError[WhoIsYourAccountantName]) and
      __.read(Reads.optionNoError[WhoIsYourAccountantIsUk]) and
      __.read(Reads.optionNoError[AccountantsAddress])
  )(WhoIsYourAccountant.apply _)

  implicit val jsonWrites: Writes[WhoIsYourAccountant] = Writes[WhoIsYourAccountant] { model =>
    Seq(
      Json.toJson(model.names).asOpt[JsObject],
      Json.toJson(model.isUk).asOpt[JsObject],
      Json.toJson(model.address).asOpt[JsObject]
    ).flatten.fold(Json.obj()) {
      _ ++ _
    }
  }
}
