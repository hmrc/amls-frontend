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

package models.status

import java.time.LocalDate

sealed trait SubmissionStatus

abstract class Renewal extends SubmissionStatus {
  val renewalDate: Option[LocalDate]
}

object NotCompleted extends SubmissionStatus
object SubmissionReady extends SubmissionStatus
object SubmissionReadyForReview extends SubmissionStatus
object SubmissionDecisionApproved extends SubmissionStatus
object SubmissionDecisionRejected extends SubmissionStatus
object SubmissionDecisionRevoked extends SubmissionStatus
object SubmissionDecisionExpired extends SubmissionStatus
object SubmissionWithdrawn extends SubmissionStatus
object DeRegistered extends SubmissionStatus
case class ReadyForRenewal(renewalDate: Option[LocalDate]) extends Renewal
case class RenewalSubmitted(renewalDate: Option[LocalDate]) extends Renewal
