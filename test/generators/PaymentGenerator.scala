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

package generators

import java.time.LocalDateTime

import models.payments.{CreateBacsPaymentRequest, Payment, PaymentStatus, PaymentStatusResult}
import models.payments.PaymentStatuses._
import org.scalacheck.Gen

//noinspection ScalaStyle
trait PaymentGenerator extends BaseGenerator with AmlsReferenceNumberGenerator {

  val refLength = 10

  def paymentRefGen: Gen[String] = stringOfLengthGen(refLength - 1) map { ref => s"X${ref.toUpperCase()}" }

  def paymentIdGen: Gen[String] = alphaNumOfLengthGen(15)

  def now: LocalDateTime = LocalDateTime.now()

  def paymentStatusGen: Gen[PaymentStatus] = Gen.oneOf(
    Created,
    Successful,
    Sent,
    Failed,
    Cancelled
  )

  val paymentGen: Gen[Payment] = for {
    _id <- alphaNumOfLengthGen(refLength)
    amlsRefNo <- amlsRefNoGen
    safeId <- amlsRefNoGen
    ref <- paymentRefGen
    desc <- alphaNumOfLengthGen(refLength)
    amountInPence <- numGen
    paymentStatus <- paymentStatusGen
  } yield Payment (
    _id,
    amlsRefNo,
    safeId,
    ref,
    desc,
    amountInPence,
    paymentStatus,
    now
  )

  val paymentStatusResultGen: Gen[PaymentStatusResult] = for {
    paymentRef <- paymentRefGen
    paymentId <- paymentIdGen
    status <- paymentStatusGen
  } yield PaymentStatusResult(paymentRef, paymentId, status)

  val createBacsPaymentGen: Gen[CreateBacsPaymentRequest] = for {
    amlsRef <- amlsRefNoGen
    safeId <- amlsRefNoGen
    payRef <- paymentRefGen
    amount <- numGen
  } yield CreateBacsPaymentRequest(amlsRef, payRef, safeId, amount)

  lazy val paymentReferenceNumber: String = paymentRefGen.sample.get

}
