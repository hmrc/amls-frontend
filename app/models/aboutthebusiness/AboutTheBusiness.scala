package models.aboutthebusiness

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap

case class AboutTheBusiness(
                             previouslyRegistered: Option[PreviouslyRegistered] = None,
                             activityStartDate: Option[ActivityStartDate] = None,
                             vatRegistered: Option[VATRegistered] = None,
                             corporationTaxRegistered: Option[CorporationTaxRegistered] = None,
                             contactingYou: Option[ContactingYou] = None,
                             registeredOffice: Option[RegisteredOffice] = None,
                             correspondenceAddress: Option[CorrespondenceAddress] = None,
                             hasChanged : Boolean = false
                           ) {

  def previouslyRegistered(v: PreviouslyRegistered): AboutTheBusiness = {
    this.copy(previouslyRegistered = Some(v), hasChanged = this.previouslyRegistered != Some(v))
  }

  def activityStartDate(v: ActivityStartDate): AboutTheBusiness =
    this.copy(activityStartDate = Some(v), hasChanged = this.activityStartDate != Some(v))

  def vatRegistered(v: VATRegistered): AboutTheBusiness =
    this.copy(vatRegistered = Some(v), hasChanged = this.vatRegistered != Some(v))

  def corporationTaxRegistered(c: CorporationTaxRegistered): AboutTheBusiness =
    this.copy(corporationTaxRegistered = Some(c), hasChanged = this.corporationTaxRegistered != Some(c))

  def registeredOffice(v: RegisteredOffice): AboutTheBusiness =
    this.copy(registeredOffice = Some(v), hasChanged = this.registeredOffice != Some(v))

  def contactingYou(v: ContactingYou): AboutTheBusiness =
    this.copy(contactingYou = Some(v), hasChanged = this.contactingYou != Some(v))

  def correspondenceAddress(v: CorrespondenceAddress): AboutTheBusiness =
    this.copy(correspondenceAddress = Some(v), hasChanged = this.correspondenceAddress != Some(v))

  def correspondenceAddress(v: Option[CorrespondenceAddress]): AboutTheBusiness =
    this.copy(correspondenceAddress = v, hasChanged = this.correspondenceAddress != v )

  def isComplete: Boolean =
    this match {
      case AboutTheBusiness(
        Some(_), _, _, _, Some(_), Some(_), _, _
      ) => true
      case _ => false
    }
}

object AboutTheBusiness {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "aboutthebusiness"
    val notStarted = Section(messageKey, NotStarted, false, controllers.aboutthebusiness.routes.WhatYouNeedController.get())
    cache.getEntry[AboutTheBusiness](key).fold(notStarted) {
      case model if model.isComplete =>
        Section(messageKey, Completed, false, controllers.aboutthebusiness.routes.SummaryController.get())
      case AboutTheBusiness(None, None, None, None, None, _, None, _) =>
        notStarted
      case _ =>
        Section(messageKey, Started, false, controllers.aboutthebusiness.routes.WhatYouNeedController.get())
    }
  }

  val key = "about-the-business"

  implicit val format =  Json.format[AboutTheBusiness]

  implicit def default(aboutTheBusiness: Option[AboutTheBusiness]): AboutTheBusiness =
    aboutTheBusiness.getOrElse(AboutTheBusiness())
}


