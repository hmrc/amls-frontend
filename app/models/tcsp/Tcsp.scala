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

package models.tcsp

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Tcsp (tcspTypes: Option[TcspTypes] = None,
                 providedServices: Option[ProvidedServices] = None,
                 doesServicesOfAnotherTCSP: Option[Boolean] = None,
                 servicesOfAnotherTCSP: Option[ServicesOfAnotherTCSP] = None,
                 hasChanged:Boolean = false,
                 hasAccepted:Boolean = false) {

  def tcspTypes(trust: TcspTypes): Tcsp =
    this.copy(tcspTypes = Some(trust), hasChanged = hasChanged || !this.tcspTypes.contains(trust), hasAccepted = hasAccepted && this.tcspTypes.contains(trust))

  def providedServices(ps: ProvidedServices): Tcsp =
    this.copy(providedServices = Some(ps), hasChanged = hasChanged || !this.providedServices.contains(ps), hasAccepted = hasAccepted && this.providedServices.contains(ps))

  def doesServicesOfAnotherTCSP(p: Boolean): Tcsp =
    this.copy(doesServicesOfAnotherTCSP = Some(p), hasChanged = hasChanged || !this.doesServicesOfAnotherTCSP.contains(p), hasAccepted = hasAccepted && this.doesServicesOfAnotherTCSP.contains(p))

  def servicesOfAnotherTCSP(p: ServicesOfAnotherTCSP): Tcsp =
    this.copy(servicesOfAnotherTCSP = Some(p), hasChanged = hasChanged || !this.servicesOfAnotherTCSP.contains(p), hasAccepted = hasAccepted && this.servicesOfAnotherTCSP.contains(p))

  def isComplete: Boolean = this match {
    case Tcsp(Some(s), t, Some(true), Some(_), _, accepted) => if(s.serviceProviders contains RegisteredOfficeEtc) { t.isDefined & accepted } else accepted
    case Tcsp(Some(s), t, Some(false), _, _, accepted) =>  if(s.serviceProviders contains RegisteredOfficeEtc) { t.isDefined & accepted } else accepted
    case Tcsp(Some(TcspTypes(serviceProviders)), _, Some(_), Some(_), _, accepted) if !serviceProviders.contains(RegisteredOfficeEtc) => accepted
    case _ => false
  }
}

object Tcsp {

  import play.api.libs.json._

  implicit val formatOption = Reads.optionWithNull[Tcsp]

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "tcsp"
    val notStarted = Section(messageKey, NotStarted, false, controllers.tcsp.routes.WhatYouNeedController.get())
    cache.getEntry[Tcsp](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, model.hasChanged, controllers.tcsp.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, model.hasChanged, controllers.tcsp.routes.WhatYouNeedController.get())
        }
    }
  }

  val key = "tcsp"

  implicit val mongoKey = new MongoKey[Tcsp] {
    override def apply(): String = "tcsp"
  }

  implicit val jsonWrites = Json.writes[Tcsp]

  def constant[A](x: A): Reads[A] = new Reads[A] {
    override def reads(json: JsValue): JsResult[A] = JsSuccess(x)
  }

  def doesServicesOfAnotherTCSPReader: Reads[Option[Boolean]] = {

    (__ \ "doesServicesOfAnotherTCSP").readNullable[Boolean] flatMap { d =>
      d match {
        case None => (__ \ "servicesOfAnotherTCSP").readNullable[ServicesOfAnotherTCSP] map { s =>

          (d, s) match {
            case (None, None) => None
            case _ => Some(s.isDefined)
          }
        }
        case p => constant(p)
      }
    }
  }

  implicit val jsonReads : Reads[Tcsp] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (__ \ "tcspTypes").readNullable[TcspTypes] and
      (__ \ "providedServices").readNullable[ProvidedServices] and
      doesServicesOfAnotherTCSPReader and
      (__ \ "servicesOfAnotherTCSP").readNullable[ServicesOfAnotherTCSP] and
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
      (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
  }.apply(Tcsp.apply _)
  implicit def default(tcsp: Option[Tcsp]): Tcsp =
    tcsp.getOrElse(Tcsp())
}