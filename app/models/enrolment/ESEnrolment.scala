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

package models.enrolment

import play.api.libs.json.Json

case class EnrolmentEntry(service: String, state: String, friendlyName: String, identifiers: Seq[EnrolmentIdentifier])
case class ESEnrolment(startRecord: Int, totalRecords: Int, enrolments: Seq[EnrolmentEntry])

object Formatters {
  implicit val entryReads = Json.reads[EnrolmentEntry]
  implicit val enrolmentReads = Json.reads[ESEnrolment]
}

