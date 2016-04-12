package models.aboutthebusiness

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import uk.gov.hmrc.http.cache.client.CacheMap

case class AboutTheBusiness(
                             previouslyRegistered: Option[PreviouslyRegistered] = None,
                             vatRegistered: Option[VATRegistered] = None,
                             contactingYou: Option[ContactingYou] = None,
                             registeredOffice: Option[RegisteredOffice] = None,
                             correspondenceAddress: Option[CorrespondenceAddress] = None
                           ) {

  def previouslyRegistered(v: PreviouslyRegistered): AboutTheBusiness =
    this.copy(previouslyRegistered = Some(v))

  def vatRegistered(v: VATRegistered): AboutTheBusiness =
    this.copy(vatRegistered = Some(v))

  def registeredOffice(v: RegisteredOffice): AboutTheBusiness =
    this.copy(registeredOffice = Some(v))

  def contactingYou(v: ContactingYou): AboutTheBusiness =
    this.copy(contactingYou = Some(v))

  def correspondenceAddress(v: CorrespondenceAddress): AboutTheBusiness =
    this.copy(correspondenceAddress = Some(v))

  def correspondenceAddress(v: Option[CorrespondenceAddress]): AboutTheBusiness =
    this.copy(correspondenceAddress = v)

  def isComplete: Boolean =
    this match {
      case AboutTheBusiness(
        Some(_), _, Some(_), Some(_), _
      ) => true
      case _ => false
    }
}

object AboutTheBusiness {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "aboutthebusiness"
    val notStarted = Section(messageKey, NotStarted, controllers.aboutthebusiness.routes.WhatYouNeedController.get())
    cache.getEntry[AboutTheBusiness](key).fold(notStarted) {
      case model if model.isComplete =>
        Section(messageKey, Completed, controllers.aboutthebusiness.routes.SummaryController.get())
      case AboutTheBusiness(None, None, None, _, None) =>
        notStarted
      case _ =>
        Section(messageKey, Started, controllers.aboutthebusiness.routes.WhatYouNeedController.get())
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "about-the-business"
  implicit val reads: Reads[AboutTheBusiness] = (
    __.read[Option[PreviouslyRegistered]] and
      __.read[Option[VATRegistered]] and
      __.read[Option[ContactingYou]] and
      __.read[Option[RegisteredOffice]] and
      __.read[Option[CorrespondenceAddress]]
    ) (AboutTheBusiness.apply _)

  implicit val writes: Writes[AboutTheBusiness] = Writes[AboutTheBusiness] {
    model =>
      Seq(
        Json.toJson(model.previouslyRegistered).asOpt[JsObject],
        Json.toJson(model.vatRegistered).asOpt[JsObject],
        Json.toJson(model.contactingYou).asOpt[JsObject],
        Json.toJson(model.registeredOffice).asOpt[JsObject],
        Json.toJson(model.correspondenceAddress).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(aboutTheBusiness: Option[AboutTheBusiness]): AboutTheBusiness =
    aboutTheBusiness.getOrElse(AboutTheBusiness())
}


