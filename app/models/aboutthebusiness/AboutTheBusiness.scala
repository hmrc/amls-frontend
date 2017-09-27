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

package models.aboutthebusiness

import config.ApplicationConfig
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.http.cache.client.CacheMap


case class AboutTheBusiness(
                             previouslyRegistered: Option[PreviouslyRegistered] = None,
                             activityStartDate: Option[ActivityStartDate] = None,
                             vatRegistered: Option[VATRegistered] = None,
                             corporationTaxRegistered: Option[CorporationTaxRegistered] = None,
                             contactingYou: Option[ContactingYou] = None,
                             registeredOffice: Option[RegisteredOffice] = None,
                             altCorrespondenceAddress: Option[Boolean] = None,
                             correspondenceAddress: Option[CorrespondenceAddress] = None,
                             hasChanged: Boolean = false,
                             hasAccepted: Boolean = false
                           ) {

  def previouslyRegistered(v: PreviouslyRegistered): AboutTheBusiness = {
    this.copy(previouslyRegistered = Some(v), hasChanged = hasChanged || !this.previouslyRegistered.contains(v), hasAccepted = hasAccepted && this.previouslyRegistered.contains(v))
  }

  def activityStartDate(v: ActivityStartDate): AboutTheBusiness =
    this.copy(activityStartDate = Some(v), hasChanged = hasChanged || !this.activityStartDate.contains(v), hasAccepted = hasAccepted && this.activityStartDate.contains(v))

  def vatRegistered(v: VATRegistered): AboutTheBusiness =
    this.copy(vatRegistered = Some(v), hasChanged = hasChanged || !this.vatRegistered.contains(v), hasAccepted = hasAccepted && this.vatRegistered.contains(v))

  def corporationTaxRegistered(c: CorporationTaxRegistered): AboutTheBusiness =
    this.copy(corporationTaxRegistered = Some(c), hasChanged = hasChanged || !this.corporationTaxRegistered.contains(c), hasAccepted = hasAccepted && this.corporationTaxRegistered.contains(c))

  def registeredOffice(v: RegisteredOffice): AboutTheBusiness =
    this.copy(registeredOffice = Some(v), hasChanged = hasChanged || !this.registeredOffice.contains(v), hasAccepted = hasAccepted && this.registeredOffice.contains(v))

  def contactingYou(v: ContactingYou): AboutTheBusiness =
    this.copy(contactingYou = Some(v), hasChanged = hasChanged || !this.contactingYou.contains(v), hasAccepted = hasAccepted && this.contactingYou.contains(v))

  def altCorrespondenceAddress(v: Boolean): AboutTheBusiness =
    this.copy(altCorrespondenceAddress = Some(v), hasChanged = hasChanged || this.altCorrespondenceAddress != Some(v))

  def correspondenceAddress(v: CorrespondenceAddress): AboutTheBusiness =
    this.copy(correspondenceAddress = Some(v), hasChanged = hasChanged || !this.correspondenceAddress.contains(v), hasAccepted = hasAccepted && this.correspondenceAddress.contains(v))

  def isComplete: Boolean =
    this match {
      case AboutTheBusiness(
      Some(_), _, _, _, Some(ContactingYou(Some(_),Some(_))), Some(_), Some(_), _,_, true
      ) if ApplicationConfig.hasAcceptedToggle => true
      case AboutTheBusiness(
      Some(_), _, _, _, Some(ContactingYou(Some(_),Some(_))), Some(_), Some(_), _, _, false
      ) if ApplicationConfig.hasAcceptedToggle => false
      case AboutTheBusiness(
      Some(_), _, _, _, Some(ContactingYou(Some(_),Some(_))), Some(_), Some(_),_, _, _) => true
      case _ => false
    }
}

object AboutTheBusiness {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "aboutthebusiness"
    val notStarted = Section(messageKey, NotStarted, false, controllers.aboutthebusiness.routes.WhatYouNeedController.get())
    cache.getEntry[AboutTheBusiness](key).fold(notStarted) {
      case model if model.isComplete =>
        Section(messageKey, Completed, model.hasChanged, controllers.aboutthebusiness.routes.SummaryController.get())
      case AboutTheBusiness(None, None, None, None, None, _, None, None, _, _) =>
        notStarted
      case model =>
        Section(messageKey, Started, model.hasChanged, controllers.aboutthebusiness.routes.WhatYouNeedController.get())
    }
  }

  val key = "about-the-business"

  implicit val format = Json.writes[AboutTheBusiness]

  implicit val jsonReads: Reads[AboutTheBusiness] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._

    (
      (__ \ "previouslyRegistered").readNullable[PreviouslyRegistered] and
        (__ \ "activityStartDate").readNullable[ActivityStartDate] and
        (__ \ "vatRegistered").readNullable[VATRegistered] and
        (__ \ "corporationTaxRegistered").readNullable[CorporationTaxRegistered] and
        (__ \ "contactingYou").readNullable[ContactingYou] and
        (__ \ "registeredOffice").readNullable[RegisteredOffice] and
        (__ \ "altCorrespondenceAddress").readNullable[Boolean] and
        (__ \ "correspondenceAddress").readNullable[CorrespondenceAddress] and
        (__ \ "hasChanged").readNullable[Boolean].map {
          _.getOrElse(false)
        } and
        (__ \ "hasAccepted").readNullable[Boolean].map {
          _.getOrElse(false)
        }
      ).apply(AboutTheBusiness.apply _)

  }

  implicit def default(aboutTheBusiness: Option[AboutTheBusiness]): AboutTheBusiness =
    aboutTheBusiness.getOrElse(AboutTheBusiness())
}


