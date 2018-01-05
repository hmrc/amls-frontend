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

package models.enrolment

import play.api.libs.json.Json

case class EnrolmentStoreEnrolment(userId: String, friendlyName: String, `type`: String, verifiers: Seq[EnrolmentIdentifier])

object EnrolmentStoreEnrolment {
  implicit val format = Json.writes[EnrolmentStoreEnrolment]

  def apply(userId: String, amlsRefNumber: String, postCode: String): EnrolmentStoreEnrolment =
    EnrolmentStoreEnrolment(userId, "AMLS Enrolment", "principal", Seq(
      EnrolmentIdentifier("Postcode", postCode)
    ))

}