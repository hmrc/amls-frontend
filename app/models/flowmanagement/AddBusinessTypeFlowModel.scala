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
import models.responsiblepeople.ResponsiblePerson
import play.api.libs.json.Json

case class AddBusinessTypeFlowModel(
                                     activity: Option[BusinessActivity] = None,
                                     areNewActivitiesAtTradingPremises: Option[Boolean] = None,
                                     tradingPremisesActivities: Option[TradingPremisesActivities] = None,
                                     addMoreActivities: Option[Boolean] = None,
                                     fitAndProper: Option[Boolean] = None,
                                     responsiblePeople: Option[ResponsiblePeopleFitAndProper] = None,
                                     hasChanged: Boolean = false,
                                     hasAccepted: Boolean = false,
                                     businessAppliedForPSRNumber: Option[BusinessAppliedForPSRNumber] = None,
                                     subSectors: Option[BusinessMatchingMsbServices] = None,
                                     tradingPremisesMsbServices: Option[BusinessMatchingMsbServices] = None
                              ) {
  def fitAndProperFromResponsiblePeople(p: Seq[ResponsiblePerson]): AddBusinessTypeFlowModel = {
    val fitAndProperInts: Set[Int] = p.zipWithIndex
            .collect({
              case (person, index) if person.hasAlreadyPassedFitAndProper.getOrElse(false) => index
            }).toSet
    val responsiblePeopleFitAndProper: Option[ResponsiblePeopleFitAndProper] = if (fitAndProperInts.nonEmpty) {
      Some(ResponsiblePeopleFitAndProper(fitAndProperInts))
    } else {
      None
    }

    this.copy(responsiblePeople = responsiblePeopleFitAndProper,
      fitAndProper = mayHavePassedFitAndProper(p),
      hasChanged = hasChanged || !this.activity.contains(p),
      hasAccepted = hasAccepted && this.activity.contains(p))
  }

  def mayHavePassedFitAndProper(p: Seq[ResponsiblePerson]): Option[Boolean] = {
    val hasTrues = p.map (_.hasAlreadyPassedFitAndProper).count(_.contains(true)) > 0
    val hasFalses = p.map (_.hasAlreadyPassedFitAndProper).count(_.contains(false)) > 0

    if(!hasTrues && !hasFalses) {
      None
    } else {
      Some(hasTrues)
    }
  }

  def activity(p: BusinessActivity): AddBusinessTypeFlowModel =
    this.copy(activity = Some(p),
      hasChanged = hasChanged || !this.activity.contains(p),
      hasAccepted = hasAccepted && this.activity.contains(p))

  def businessAppliedForPSRNumber(p: BusinessAppliedForPSRNumber): AddBusinessTypeFlowModel =
    this.copy(businessAppliedForPSRNumber = Some(p),
      hasChanged = hasChanged || !this.businessAppliedForPSRNumber.contains(p),
      hasAccepted = hasAccepted && this.businessAppliedForPSRNumber.contains(p))

  def msbServices(p: BusinessMatchingMsbServices): AddBusinessTypeFlowModel = {
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

    this.copy(subSectors = Some(p),
      businessAppliedForPSRNumber = businessAppliedForPSRNumber,
      tradingPremisesMsbServices = tradingPremisesMsbActivities,
      hasChanged = hasChanged || !this.subSectors.contains(p),
      hasAccepted = hasAccepted && this.subSectors.contains(p))
  }

  def tradingPremisesMsbServices(p: BusinessMatchingMsbServices): AddBusinessTypeFlowModel = {
    this.copy(tradingPremisesMsbServices = Some(p),
      hasChanged = hasChanged || !this.tradingPremisesMsbServices.contains(p),
      hasAccepted = hasAccepted && this.tradingPremisesMsbServices.contains(p))
  }

  def isActivityAtTradingPremises(p: Option[Boolean]): AddBusinessTypeFlowModel =
    this.copy(areNewActivitiesAtTradingPremises = p,
      hasChanged = hasChanged || !this.areNewActivitiesAtTradingPremises.equals(p),
      hasAccepted = hasAccepted && this.areNewActivitiesAtTradingPremises.equals(p))

  def tradingPremisesActivities(p: Option[TradingPremisesActivities]): AddBusinessTypeFlowModel =
    this.copy(tradingPremisesActivities = p,
      hasChanged = hasChanged || !this.tradingPremisesActivities.equals(p),
      hasAccepted = hasAccepted && this.tradingPremisesActivities.equals(p))

  def isfitAndProper(p: Option[Boolean]): AddBusinessTypeFlowModel =
    this.copy(fitAndProper = p,
      hasChanged = hasChanged || !this.fitAndProper.equals(p),
      hasAccepted = hasAccepted && this.fitAndProper.equals(p))

  def responsiblePeople(p: Option[ResponsiblePeopleFitAndProper]): AddBusinessTypeFlowModel =
    this.copy(responsiblePeople = p,
      hasChanged = hasChanged || !this.responsiblePeople.equals(p),
      hasAccepted = hasAccepted && this.responsiblePeople.equals(p))

  def isComplete: Boolean = this match {
    case AddBusinessTypeFlowModel(Some(MoneyServiceBusiness), Some(_), Some(_), Some(_), Some(true), Some(_), _, true, _, _, _) => true
    case AddBusinessTypeFlowModel(Some(MoneyServiceBusiness), Some(false), _, Some(_), Some(false), _, _, true, _, _, _) => true
    case AddBusinessTypeFlowModel(Some(TrustAndCompanyServices), Some(_), Some(_), Some(_), Some(true), Some(_), _, true, _, _, _) => true
    case AddBusinessTypeFlowModel(Some(TrustAndCompanyServices), Some(false), _, Some(_), Some(false), _, _, true, _, _, _) => true
    case AddBusinessTypeFlowModel(Some(_), Some(_), Some(_), Some(_), Some(_), _, _, true, _, _, _) => true
    case AddBusinessTypeFlowModel(Some(_), Some(false), _, Some(_), Some(_), _, _, true, _, _, _) => true


    case _ => false
  }

  def informationRequired = this.activity.exists {
    case BillPaymentServices | TelephonePaymentService => false
    case _ => true
  }

  def activityName = this.activity map { _.getMessage() }

}

object AddBusinessTypeFlowModel {

  val key = "add-service-flow"

  implicit val addServiceFlowModelFormat = Json.format[AddBusinessTypeFlowModel]

}
