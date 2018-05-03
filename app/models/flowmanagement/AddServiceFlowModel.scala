/*
 * Copyright 2018 HM Revenue & Customs
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

package models.flowmanagement

import models.businessmatching._
import models.businessmatching.updateservice.{ResponsiblePeopleFitAndProper, TradingPremisesActivities}
import models.responsiblepeople.ResponsiblePeople
import play.api.libs.json.Json

case class AddServiceFlowModel(
                                activity: Option[BusinessActivity] = None,
                                areNewActivitiesAtTradingPremises: Option[Boolean] = None,
                                tradingPremisesActivities: Option[TradingPremisesActivities] = None,
                                addMoreActivities: Option[Boolean] = None,
                                fitAndProper: Option[Boolean] = None,
                                responsiblePeople: Option[ResponsiblePeopleFitAndProper] = None,
                                hasChanged: Boolean = false,
                                hasAccepted: Boolean = false,
                                businessAppliedForPSRNumber: Option[BusinessAppliedForPSRNumber] = None,
                                msbServices: Option[BusinessMatchingMsbServices] = None,
                                tradingPremisesMsbServices: Option[BusinessMatchingMsbServices] = None
                              ) {
  def empty(): Boolean = this match {
    case AddServiceFlowModel(_, None, None, None, None, None, false, false, None, None, None) => true
    case _ => false
  }

  def fitAndProperFromResponsiblePeople(p: Seq[ResponsiblePeople]): AddServiceFlowModel = {
    val fitAndProperInts: Set[Int] = p.zipWithIndex
            .filter(personWithIndex => personWithIndex._1.hasAlreadyPassedFitAndProper.getOrElse(false))
            .map(personWithIndex => personWithIndex._2).toSet

    val responsiblePeopleFitAndProper: Option[ResponsiblePeopleFitAndProper] = if (fitAndProperInts.nonEmpty) {
      Some(ResponsiblePeopleFitAndProper(fitAndProperInts))
    } else {
      None
    }

    this.copy(responsiblePeople = responsiblePeopleFitAndProper,
      fitAndProper = calculateFitAndProper(p),
      hasChanged = hasChanged || !this.activity.contains(p),
      hasAccepted = hasAccepted && this.activity.contains(p))
  }

  def calculateFitAndProper(p: Seq[ResponsiblePeople]):Option [Boolean] = {
    val hasTrues = p.map (pt=> pt.hasAlreadyPassedFitAndProper).count(_ == Some(true)) > 0
    val hasFalses = p.map (pt=> pt.hasAlreadyPassedFitAndProper).count(_ == Some(false)) > 0

    if(!hasTrues && !hasFalses) {
      None
    } else {
      Some(hasTrues)
    }

  }

  def activity(p: BusinessActivity): AddServiceFlowModel =
    this.copy(activity = Some(p),
      hasChanged = hasChanged || !this.activity.contains(p),
      hasAccepted = hasAccepted && this.activity.contains(p))

  def businessAppliedForPSRNumber(p: BusinessAppliedForPSRNumber): AddServiceFlowModel =
    this.copy(businessAppliedForPSRNumber = Some(p),
      hasChanged = hasChanged || !this.businessAppliedForPSRNumber.contains(p),
      hasAccepted = hasAccepted && this.businessAppliedForPSRNumber.contains(p))

  def msbServices(p: BusinessMatchingMsbServices): AddServiceFlowModel = {
    val intersectingMsbServices: Set[BusinessMatchingMsbService] =
      this.tradingPremisesMsbServices.getOrElse(BusinessMatchingMsbServices(Set())).msbServices.intersect(p.msbServices)
    val tradingPremisesMsbActivities: Option[BusinessMatchingMsbServices] =
      if (this.tradingPremisesMsbServices.isDefined && intersectingMsbServices.nonEmpty) {
        Some(BusinessMatchingMsbServices(intersectingMsbServices))
      } else if (p.msbServices.size == 1) {
        Some(BusinessMatchingMsbServices(p.msbServices))
      } else {
        None
      }
    val businessAppliedForPSRNumber: Option[BusinessAppliedForPSRNumber] =
      if (p.msbServices.contains(TransmittingMoney)) {
        this.businessAppliedForPSRNumber
      } else {
        None
      }

    this.copy(msbServices = Some(p),
      businessAppliedForPSRNumber = businessAppliedForPSRNumber,
      tradingPremisesMsbServices = tradingPremisesMsbActivities,
      hasChanged = hasChanged || !this.msbServices.contains(p),
      hasAccepted = hasAccepted && this.msbServices.contains(p))
  }

  def tradingPremisesMsbServices(p: BusinessMatchingMsbServices): AddServiceFlowModel = {
    this.copy(tradingPremisesMsbServices = Some(p),
      hasChanged = hasChanged || !this.tradingPremisesMsbServices.contains(p),
      hasAccepted = hasAccepted && this.tradingPremisesMsbServices.contains(p))
  }

  def isActivityAtTradingPremises(p: Option[Boolean]): AddServiceFlowModel =
    this.copy(areNewActivitiesAtTradingPremises = p,
      hasChanged = hasChanged || !this.areNewActivitiesAtTradingPremises.equals(p),
      hasAccepted = hasAccepted && this.areNewActivitiesAtTradingPremises.equals(p))

  def tradingPremisesActivities(p: Option[TradingPremisesActivities]): AddServiceFlowModel =
    this.copy(tradingPremisesActivities = p,
      hasChanged = hasChanged || !this.tradingPremisesActivities.equals(p),
      hasAccepted = hasAccepted && this.tradingPremisesActivities.equals(p))

  def isfitAndProper(p: Option[Boolean]): AddServiceFlowModel =
    this.copy(fitAndProper = p,
      hasChanged = hasChanged || !this.fitAndProper.equals(p),
      hasAccepted = hasAccepted && this.fitAndProper.equals(p))

  def responsiblePeople(p: Option[ResponsiblePeopleFitAndProper]): AddServiceFlowModel =
    this.copy(responsiblePeople = p,
      hasChanged = hasChanged || !this.responsiblePeople.equals(p),
      hasAccepted = hasAccepted && this.responsiblePeople.equals(p))

  def isComplete: Boolean = this match {
    case AddServiceFlowModel(Some(MoneyServiceBusiness), Some(_), Some(_), Some(_), Some(true), Some(_), _, true, _, _, _) => true
    case AddServiceFlowModel(Some(MoneyServiceBusiness), Some(false), _, Some(_), Some(false), _, _, true, _, _, _) => true
    case AddServiceFlowModel(Some(TrustAndCompanyServices), Some(_), Some(_), Some(_), Some(true), Some(_), _, true, _, _, _) => true
    case AddServiceFlowModel(Some(TrustAndCompanyServices), Some(false), _, Some(_), Some(false), _, _, true, _, _, _) => true
    case AddServiceFlowModel(Some(_), Some(_), Some(_), Some(_), Some(_), _, _, true, _, _, _) => true
    case AddServiceFlowModel(Some(_), Some(false), _, Some(_), Some(_), _, _, true, _, _, _) => true


    case _ => false
  }

  def informationRequired = this.activity.exists {
    case BillPaymentServices | TelephonePaymentService => false
    case _ => true
  }

  def activityName = this.activity map { _.getMessage }

}

object AddServiceFlowModel {

  val key = "add-service-flow"

  implicit val addServiceFlowModelFormat = Json.format[AddServiceFlowModel]

}
