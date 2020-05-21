/*
 * Copyright 2020 HM Revenue & Customs
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

case object AddMoreBusinessTypesPageId extends PageId
case object PsrNumberPageId extends PageId
case object NeedMoreInformationPageId extends PageId
case object NoPSRPageId extends PageId
case object SelectBusinessTypesPageId extends PageId
case object SubSectorsPageId extends PageId
case object AddBusinessTypeSummaryPageId extends PageId
case object ChangeBusinessTypesPageId extends PageId
case object WhatBusinessTypesToRemovePageId extends PageId
case object NeedToUpdatePageId extends PageId
case object RemoveBusinessTypesSummaryPageId extends PageId
case object UnableToRemovePageId extends PageId
case object WhatDateRemovedPageId extends PageId
