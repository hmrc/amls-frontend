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

package models.businessdetails

import models.registrationprogress._
import play.api.i18n.Messages
import play.api.libs.json.{Json, OWrites, Reads}
import services.cache.Cache

case class BusinessDetails(
  previouslyRegistered: Option[PreviouslyRegistered] = None,
  activityStartDate: Option[ActivityStartDate] = None,
  vatRegistered: Option[VATRegistered] = None,
  corporationTaxRegistered: Option[CorporationTaxRegistered] = None,
  contactingYou: Option[ContactingYou] = None,
  registeredOfficeIsUK: Option[RegisteredOfficeIsUK] = None,
  registeredOffice: Option[RegisteredOffice] = None,
  altCorrespondenceAddress: Option[Boolean] = None,
  correspondenceAddressIsUk: Option[CorrespondenceAddressIsUk] = None,
  correspondenceAddress: Option[CorrespondenceAddress] = None,
  hasChanged: Boolean = false,
  hasAccepted: Boolean = false
) {

  def previouslyRegistered(v: PreviouslyRegistered): BusinessDetails =
    this.copy(
      previouslyRegistered = Some(v),
      hasChanged = hasChanged || !this.previouslyRegistered.contains(v),
      hasAccepted = hasAccepted && this.previouslyRegistered.contains(v)
    )

  def activityStartDate(v: ActivityStartDate): BusinessDetails =
    this.copy(
      activityStartDate = Some(v),
      hasChanged = hasChanged || !this.activityStartDate.contains(v),
      hasAccepted = hasAccepted && this.activityStartDate.contains(v)
    )

  def vatRegistered(v: VATRegistered): BusinessDetails =
    this.copy(
      vatRegistered = Some(v),
      hasChanged = hasChanged || !this.vatRegistered.contains(v),
      hasAccepted = hasAccepted && this.vatRegistered.contains(v)
    )

  def corporationTaxRegistered(c: CorporationTaxRegistered): BusinessDetails =
    this.copy(
      corporationTaxRegistered = Some(c),
      hasChanged = hasChanged || !this.corporationTaxRegistered.contains(c),
      hasAccepted = hasAccepted && this.corporationTaxRegistered.contains(c)
    )

  def registeredOfficeIsUK(v: RegisteredOfficeIsUK): BusinessDetails =
    this.copy(
      registeredOfficeIsUK = Some(v),
      hasChanged = hasChanged || !this.registeredOfficeIsUK.contains(v),
      hasAccepted = hasAccepted && this.registeredOfficeIsUK.contains(v)
    )

  def registeredOffice(v: RegisteredOffice): BusinessDetails =
    this.copy(
      registeredOffice = Some(v),
      hasChanged = hasChanged || !this.registeredOffice.contains(v),
      hasAccepted = hasAccepted && this.registeredOffice.contains(v)
    )

  def contactingYou(v: ContactingYou): BusinessDetails =
    this.copy(
      contactingYou = Some(v),
      hasChanged = hasChanged || !this.contactingYou.contains(v),
      hasAccepted = hasAccepted && this.contactingYou.contains(v)
    )

  def altCorrespondenceAddress(v: Boolean): BusinessDetails =
    this.copy(altCorrespondenceAddress = Some(v), hasChanged = hasChanged || !this.altCorrespondenceAddress.contains(v))

  def correspondenceAddress(v: CorrespondenceAddress): BusinessDetails =
    v match {
      case CorrespondenceAddress(None, None) =>
        this.copy(
          correspondenceAddress = None,
          hasChanged = hasChanged || !this.correspondenceAddress.contains(v),
          hasAccepted = hasAccepted && this.correspondenceAddress.contains(v)
        )
      case _                                 =>
        this.copy(
          correspondenceAddress = Some(v),
          hasChanged = hasChanged || !this.correspondenceAddress.contains(v),
          hasAccepted = hasAccepted && this.correspondenceAddress.contains(v)
        )
    }

  def correspondenceAddressIsUk(v: CorrespondenceAddressIsUk): BusinessDetails =
    this.copy(
      correspondenceAddressIsUk = Some(v),
      hasChanged = hasChanged || !this.correspondenceAddressIsUk.contains(v),
      hasAccepted = hasAccepted && this.correspondenceAddressIsUk.contains(v)
    )

  def isComplete: Boolean =
    this match {
      case BusinessDetails(Some(PreviouslyRegisteredYes(None)), None, _, _, _, _, _, _, _, _, _, _) =>
        false
      case BusinessDetails(Some(PreviouslyRegisteredNo), None, _, _, _, _, _, _, _, _, _, _)        =>
        false
      case BusinessDetails(
            Some(_),
            _,
            _,
            _,
            Some(ContactingYou(Some(_), Some(_))),
            _,
            Some(_),
            Some(true),
            _,
            None,
            _,
            true
          ) =>
        false
      case BusinessDetails(
            Some(_),
            _,
            _,
            _,
            Some(ContactingYou(Some(_), Some(_))),
            _,
            Some(_),
            Some(_),
            _,
            _,
            _,
            true
          ) =>
        true
      case _                                                                                        =>
        false
    }
}

object BusinessDetails {

  def taskRow(implicit cache: Cache, messages: Messages) = {

    val messageKey = "businessdetails"
    val notStarted = TaskRow(
      messageKey,
      controllers.businessdetails.routes.WhatYouNeedController.get.url,
      hasChanged = false,
      NotStarted,
      TaskRow.notStartedTag
    )

    cache.getEntry[BusinessDetails](key).fold(notStarted) {
      case model if model.isComplete                                                      =>
        TaskRow(
          messageKey,
          controllers.businessdetails.routes.SummaryController.get.url,
          model.hasChanged,
          Completed,
          TaskRow.completedTag
        )
      case BusinessDetails(None, None, None, None, None, _, None, None, None, None, _, _) =>
        notStarted
      case model                                                                          =>
        TaskRow(
          messageKey,
          controllers.businessdetails.routes.WhatYouNeedController.get.url,
          model.hasChanged,
          Started,
          TaskRow.incompleteTag
        )
    }
  }

  val key = "about-the-business"

  implicit val format: OWrites[BusinessDetails] = Json.writes[BusinessDetails]

  implicit val jsonReads: Reads[BusinessDetails] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._

    (
      (__ \ "previouslyRegistered").readNullable[PreviouslyRegistered] and
        (__ \ "activityStartDate").readNullable[ActivityStartDate] and
        (__ \ "vatRegistered").readNullable[VATRegistered] and
        (__ \ "corporationTaxRegistered").readNullable[CorporationTaxRegistered] and
        (__ \ "contactingYou").readNullable[ContactingYou] and
        (__ \ "registeredOfficeIsUK").readNullable[RegisteredOfficeIsUK] and
        (__ \ "registeredOffice").readNullable[RegisteredOffice] and
        (__ \ "altCorrespondenceAddress").readNullable[Boolean] and
        (__ \ "correspondenceAddressIsUk").readNullable[CorrespondenceAddressIsUk] and
        (__ \ "correspondenceAddress").readNullable[CorrespondenceAddress] and
        (__ \ "hasChanged").readNullable[Boolean].map {
          _.getOrElse(false)
        } and
        (__ \ "hasAccepted").readNullable[Boolean].map {
          _.getOrElse(false)
        }
    ).apply(BusinessDetails.apply _)

  }

  implicit def default(businessDetails: Option[BusinessDetails]): BusinessDetails =
    businessDetails.getOrElse(BusinessDetails())
}
