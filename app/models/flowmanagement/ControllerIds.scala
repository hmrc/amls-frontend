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

sealed trait PageId

sealed trait AddServiceFlowPageId extends PageId

case object WhatDoYouWantToDoPageId extends AddServiceFlowPageId
case object BusinessActivitiesSelectionPageId extends AddServiceFlowPageId
case object TradingPremisesDecisionPageId extends AddServiceFlowPageId
case object TradingPremisesSelectionPageId extends AddServiceFlowPageId
case object AddServiceSummaryPageId extends AddServiceFlowPageId
case object NewServiceInformationPageId extends AddServiceFlowPageId

sealed trait RemoveServiceFlowPageId extends PageId
case object WhatServiceToRemovePageId extends RemoveServiceFlowPageId
case object WhatDateRemovedPageId extends RemoveServiceFlowPageId
case object NeedToUpdatePageId extends RemoveServiceFlowPageId