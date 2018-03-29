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

// Add service flow
case object WhatDoYouWantToDoPageId extends PageId
case object BusinessActivitiesSelectionPageId extends PageId
case object TradingPremisesDecisionPageId extends PageId
case object TradingPremisesSelectionPageId extends PageId
case object AddServiceSummaryPageId extends PageId
case object NewServiceInformationPageId extends PageId

// Remove service flow
case object WhatServiceToRemovePageId extends PageId
case object WhatDateRemovedPageId extends PageId
case object NeedToUpdatePageId extends PageId