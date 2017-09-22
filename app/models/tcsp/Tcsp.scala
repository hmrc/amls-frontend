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

import config.ApplicationConfig
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.collection.Seq

case class Tcsp (tcspTypes: Option[TcspTypes] = None,
                 providedServices: Option[ProvidedServices] = None,
                 servicesOfAnotherTCSP: Option[ServicesOfAnotherTCSP] = None,
                 hasChanged:Boolean = false,
                 hasAccepted:Boolean = false) {

  def tcspTypes(trust: TcspTypes): Tcsp =
    this.copy(tcspTypes = Some(trust), hasChanged = hasChanged || !this.tcspTypes.contains(trust), hasAccepted = hasAccepted && this.tcspTypes.contains(trust))

  def providedServices(ps: ProvidedServices): Tcsp =
    this.copy(providedServices = Some(ps), hasChanged = hasChanged || !this.providedServices.contains(ps), hasAccepted = hasAccepted && this.providedServices.contains(ps))

  def servicesOfAnotherTCSP(p: ServicesOfAnotherTCSP): Tcsp =
    this.copy(servicesOfAnotherTCSP = Some(p), hasChanged = hasChanged || !this.servicesOfAnotherTCSP.contains(p), hasAccepted = hasAccepted && this.servicesOfAnotherTCSP.contains(p))

  def isComplete: Boolean = if(ApplicationConfig.hasAcceptedToggle) {
    this match {
      case Tcsp(Some(_), Some(_), Some(_), _, true) => true
      case Tcsp(Some(_), Some(_), Some(_), _, false) => false
      case Tcsp(Some(TcspTypes(serviceProviders)), _, Some(_), _, false) if !serviceProviders.contains(RegisteredOfficeEtc) => false
      case Tcsp(Some(TcspTypes(serviceProviders)), _, Some(_), _, true) if !serviceProviders.contains(RegisteredOfficeEtc) => true
      case _ => false
    }
  } else {
    this match {
      case Tcsp(Some(_), Some(_), Some(_), _, _) => true
      case Tcsp(Some(TcspTypes(serviceProviders)), _, Some(_), _, _) if !serviceProviders.contains(RegisteredOfficeEtc) => true
      case _ => false
    }
  }
}

object Tcsp {

  import play.api.libs.functional.syntax._
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

  implicit val jsonReads : Reads[Tcsp] = {
    (__ \ "tcspTypes").readNullable[TcspTypes] and
      (__ \ "providedServices").readNullable[ProvidedServices] and
      (__ \ "servicesOfAnotherTCSP").readNullable[ServicesOfAnotherTCSP] and
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
      (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
  }.apply(Tcsp.apply _)
  implicit def default(tcsp: Option[Tcsp]): Tcsp =
    tcsp.getOrElse(Tcsp())
}
