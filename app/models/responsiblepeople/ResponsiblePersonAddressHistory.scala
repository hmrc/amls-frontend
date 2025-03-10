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

import play.api.libs.json.{Json, OFormat}

case class ResponsiblePersonAddressHistory(
  currentAddress: Option[ResponsiblePersonCurrentAddress] = None,
  additionalAddress: Option[ResponsiblePersonAddress] = None,
  additionalExtraAddress: Option[ResponsiblePersonAddress] = None
) {

  def currentAddress(add: ResponsiblePersonCurrentAddress): ResponsiblePersonAddressHistory =
    this.copy(currentAddress = Some(add))

  def additionalAddress(add: ResponsiblePersonAddress): ResponsiblePersonAddressHistory =
    this.copy(additionalAddress = Some(add))

  def additionalExtraAddress(add: ResponsiblePersonAddress): ResponsiblePersonAddressHistory =
    this.copy(additionalExtraAddress = Some(add))

  def removeAdditionalExtraAddress = this.copy(additionalExtraAddress = None)

  def isComplete: Boolean = currentAddress.isDefined
}

object ResponsiblePersonAddressHistory {

  implicit val format: OFormat[ResponsiblePersonAddressHistory] = Json.format[ResponsiblePersonAddressHistory]

  def default(): ResponsiblePersonCurrentAddress =
    ResponsiblePersonCurrentAddress(PersonAddressUK("", None, None, None, ""), None)

  def isRPAddressInUK(address: Option[ResponsiblePersonAddress]): Boolean =
    address match {
      case Some(ResponsiblePersonAddress(PersonAddressUK(_, _, _, _, _), _))    => true
      case Some(ResponsiblePersonAddress(PersonAddressNonUK(_, _, _, _, _), _)) => false
      case None                                                                 => false
    }

  def isRPCurrentAddressInUK(address: Option[ResponsiblePersonCurrentAddress]): Boolean =
    address match {
      case Some(ResponsiblePersonCurrentAddress(PersonAddressUK(_, _, _, _, _), _, _))    => true
      case Some(ResponsiblePersonCurrentAddress(PersonAddressNonUK(_, _, _, _, _), _, _)) => false
      case None                                                                           => false
    }
}
