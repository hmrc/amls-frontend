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

package models.flowmanagement

import models.businessmatching._
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}

case class AddBusinessTypeFlowModel(
  activity: Option[BusinessActivity] = None,
  addMoreActivities: Option[Boolean] = None,
  hasChanged: Boolean = false,
  hasAccepted: Boolean = false,
  businessAppliedForPSRNumber: Option[BusinessAppliedForPSRNumber] = None,
  subSectors: Option[BusinessMatchingMsbServices] = None
) {

  def activity(p: BusinessActivity): AddBusinessTypeFlowModel =
    this.copy(
      activity = Some(p),
      hasChanged = hasChanged || !this.activity.contains(p),
      hasAccepted = hasAccepted && this.activity.contains(p)
    )

  def businessAppliedForPSRNumber(p: BusinessAppliedForPSRNumber): AddBusinessTypeFlowModel =
    this.copy(
      businessAppliedForPSRNumber = Some(p),
      hasChanged = hasChanged || !this.businessAppliedForPSRNumber.contains(p),
      hasAccepted = hasAccepted && this.businessAppliedForPSRNumber.contains(p)
    )

  def msbServices(p: BusinessMatchingMsbServices): AddBusinessTypeFlowModel = {
    val businessAppliedForPSRNumber: Option[BusinessAppliedForPSRNumber] =
      if (p.msbServices.contains(TransmittingMoney)) {
        this.businessAppliedForPSRNumber
      } else {
        None
      }

    this.copy(
      subSectors = Some(p),
      businessAppliedForPSRNumber = businessAppliedForPSRNumber,
      hasChanged = hasChanged || !this.subSectors.contains(p),
      hasAccepted = hasAccepted && this.subSectors.contains(p)
    )
  }

  def isComplete: Boolean = this match {
    case AddBusinessTypeFlowModel(Some(MoneyServiceBusiness), Some(_), _, true, Some(_), _) => true
    case AddBusinessTypeFlowModel(Some(_), Some(_), _, true, _, _)                          => true
    case _                                                                                  => false
  }

  def informationRequired = this.activity.exists {
    case BillPaymentServices | TelephonePaymentService => false
    case _                                             => true
  }

  def activityName(implicit messages: Messages) = this.activity map {
    _.getMessage()
  }

  def isMsbTmDefined: Boolean = this.activity.exists {
    case MoneyServiceBusiness if this.subSectors.fold[Boolean](false)(_.msbServices.contains(TransmittingMoney)) => true
    case _                                                                                                       => false
  }

}

object AddBusinessTypeFlowModel {

  val key = "add-service-flow"

  implicit val addServiceFlowModelFormat: OFormat[AddBusinessTypeFlowModel] = Json.format[AddBusinessTypeFlowModel]

}
