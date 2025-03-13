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

package models.tcsp

import models.registrationprogress._
import models.tcsp.TcspTypes._
import play.api.i18n.Messages
import typeclasses.MongoKey
import services.cache.Cache

case class Tcsp(
  tcspTypes: Option[TcspTypes] = None,
  onlyOffTheShelfCompsSold: Option[OnlyOffTheShelfCompsSold] = None,
  complexCorpStructureCreation: Option[ComplexCorpStructureCreation] = None,
  providedServices: Option[ProvidedServices] = None,
  doesServicesOfAnotherTCSP: Option[Boolean] = None,
  servicesOfAnotherTCSP: Option[ServicesOfAnotherTCSP] = None,
  hasChanged: Boolean = false,
  hasAccepted: Boolean = false
) {

  def tcspTypes(trust: TcspTypes): Tcsp =
    this.copy(
      tcspTypes = Some(trust),
      hasChanged = hasChanged || !this.tcspTypes.contains(trust),
      hasAccepted = hasAccepted && this.tcspTypes.contains(trust)
    )

  def onlyOffTheShelfCompsSold(onlyOffTheShelf: OnlyOffTheShelfCompsSold): Tcsp =
    this.copy(
      onlyOffTheShelfCompsSold = Some(onlyOffTheShelf),
      hasChanged = hasChanged || !this.onlyOffTheShelfCompsSold.contains(onlyOffTheShelf),
      hasAccepted = hasAccepted && this.onlyOffTheShelfCompsSold.contains(onlyOffTheShelf)
    )

  def complexCorpStructureCreation(complexCorp: ComplexCorpStructureCreation): Tcsp =
    this.copy(
      complexCorpStructureCreation = Some(complexCorp),
      hasChanged = hasChanged || !this.complexCorpStructureCreation.contains(complexCorp),
      hasAccepted = hasAccepted && this.complexCorpStructureCreation.contains(complexCorp)
    )

  def providedServices(ps: ProvidedServices): Tcsp =
    this.copy(
      providedServices = Some(ps),
      hasChanged = hasChanged || !this.providedServices.contains(ps),
      hasAccepted = hasAccepted && this.providedServices.contains(ps)
    )

  def doesServicesOfAnotherTCSP(p: Boolean): Tcsp =
    this.copy(
      doesServicesOfAnotherTCSP = Some(p),
      hasChanged = hasChanged || !this.doesServicesOfAnotherTCSP.contains(p),
      hasAccepted = hasAccepted && this.doesServicesOfAnotherTCSP.contains(p)
    )

  def servicesOfAnotherTCSP(p: ServicesOfAnotherTCSP): Tcsp =
    this.copy(
      servicesOfAnotherTCSP = Some(p),
      hasChanged = hasChanged || !this.servicesOfAnotherTCSP.contains(p),
      hasAccepted = hasAccepted && this.servicesOfAnotherTCSP.contains(p)
    )

  def hasRegisteredOfficeEtc(s: TcspTypes) =
    s.serviceProviders.contains(RegisteredOfficeEtc)

  def hasCompanyFormationAgent(s: TcspTypes) =
    s.serviceProviders.contains(CompanyFormationAgent)

  def completeWithCompanyFormationAgent: Boolean = this match {
    case Tcsp(Some(s), Some(_), Some(_), t, Some(true), Some(_), _, accepted) =>
      if (hasRegisteredOfficeEtc(s)) { t.isDefined & accepted }
      else accepted
    case Tcsp(Some(s), Some(_), Some(_), t, Some(false), _, _, accepted)      =>
      if (hasRegisteredOfficeEtc(s)) { t.isDefined & accepted }
      else accepted
    case _                                                                    => false
  }

  def completeWithoutCompanyFormationAgent: Boolean = this match {
    case Tcsp(Some(s), _, _, t, Some(true), Some(_), _, accepted) =>
      if (hasRegisteredOfficeEtc(s)) { t.isDefined & accepted }
      else accepted
    case Tcsp(Some(s), _, _, t, Some(false), _, _, accepted)      =>
      if (hasRegisteredOfficeEtc(s)) { t.isDefined & accepted }
      else accepted
    case _                                                        => false
  }

  def isComplete: Boolean =
    this.tcspTypes match {
      case Some(s) =>
        if (hasCompanyFormationAgent(s)) {
          completeWithCompanyFormationAgent
        } else {
          completeWithoutCompanyFormationAgent
        }
      case _       => false
    }
}

object Tcsp {

  import play.api.libs.json._
  import utils.MappingUtils._

  def taskRow(implicit cache: Cache, messages: Messages): TaskRow = {
    val notStarted = TaskRow(
      key,
      controllers.tcsp.routes.WhatYouNeedController.get().url,
      hasChanged = false,
      NotStarted,
      TaskRow.notStartedTag
    )
    cache.getEntry[Tcsp](key).fold(notStarted) { model =>
      if (model.isComplete && model.hasChanged) {
        TaskRow(
          key,
          controllers.tcsp.routes.SummaryController.get().url,
          hasChanged = true,
          status = Updated,
          tag = TaskRow.updatedTag
        )
      } else if (model.isComplete) {
        TaskRow(
          key,
          controllers.tcsp.routes.SummaryController.get().url,
          model.hasChanged,
          Completed,
          TaskRow.completedTag
        )
      } else {
        TaskRow(
          key,
          controllers.tcsp.routes.WhatYouNeedController.get().url,
          model.hasChanged,
          Started,
          TaskRow.incompleteTag
        )
      }
    }
  }

  val key = "tcsp"

  implicit val mongoKey: MongoKey[Tcsp] = () => "tcsp"

  implicit val jsonWrites: OWrites[Tcsp] = Json.writes[Tcsp]

  def doesServicesOfAnotherTCSPReader: Reads[Option[Boolean]] =
    (__ \ "doesServicesOfAnotherTCSP").readNullable[Boolean] flatMap {
      case d @ None =>
        (__ \ "servicesOfAnotherTCSP").readNullable[ServicesOfAnotherTCSP] map { s =>
          (d, s) match {
            case (None, None) => None
            case _            => Some(s.isDefined)
          }
        }
      case p        => constant(p)
    }

  def oldOnlyOffTheShelfCompsSoldReader: Reads[Option[OnlyOffTheShelfCompsSold]] =
    (__ \ "tcspTypes" \ "onlyOffTheShelfCompsSold").readNullable[Boolean] map {
      case Some(true)  => Some(OnlyOffTheShelfCompsSoldYes)
      case Some(false) => Some(OnlyOffTheShelfCompsSoldNo)
      case _           => None
    }

  def oldComplexCorpStructureCreationReader: Reads[Option[ComplexCorpStructureCreation]] =
    (__ \ "tcspTypes" \ "complexCorpStructureCreation").readNullable[Boolean] map {
      case Some(true)  => Some(ComplexCorpStructureCreationYes)
      case Some(false) => Some(ComplexCorpStructureCreationNo)
      case _           => None
    }

  implicit val jsonReads: Reads[Tcsp]            = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (__ \ "tcspTypes").readNullable[TcspTypes] and
      ((__ \ "onlyOffTheShelfCompsSold").readNullable[OnlyOffTheShelfCompsSold] flatMap {
        case None => oldOnlyOffTheShelfCompsSoldReader
        case x    => constant(x)
      }) and
      ((__ \ "complexCorpStructureCreation").readNullable[ComplexCorpStructureCreation] flatMap {
        case None => oldComplexCorpStructureCreationReader
        case x    => constant(x)
      }) and
      (__ \ "providedServices").readNullable[ProvidedServices] and
      doesServicesOfAnotherTCSPReader and
      (__ \ "servicesOfAnotherTCSP").readNullable[ServicesOfAnotherTCSP] and
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
      (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
  }.apply(Tcsp.apply _)
  implicit def default(tcsp: Option[Tcsp]): Tcsp =
    tcsp.getOrElse(Tcsp())

  implicit val formatOption: Reads[Option[Tcsp]] = Reads.optionWithNull[Tcsp]

}
