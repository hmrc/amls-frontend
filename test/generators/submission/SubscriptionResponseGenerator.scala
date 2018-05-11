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

package generators.submission

import generators.{AmlsReferenceNumberGenerator, BaseGenerator, PaymentGenerator}
import models.{SubscriptionFees, SubscriptionResponse}
import org.scalacheck.Gen

// scalastyle:off magic.number
trait SubscriptionResponseGenerator extends BaseGenerator
  with AmlsReferenceNumberGenerator
  with PaymentGenerator {

  val subscriptionFeesGen: Gen[SubscriptionFees] = for {
    paymentReference <- paymentRefGen
    fees <- Gen.choose(100, 500)
  } yield {
    SubscriptionFees(paymentReference, BigDecimal(100), Some(115), Some(115), BigDecimal(130), Some(130), BigDecimal(fees))
  }

  def subscriptionResponseGen(hasFees: Boolean = false): Gen[SubscriptionResponse] = for {
    regNo <- amlsRefNoGen
    formBundleNumber <- numSequence(10)
    fees <- subscriptionFeesGen
  } yield {
    SubscriptionResponse(
      formBundleNumber.toString,
      regNo,
      if (hasFees) Some(fees) else None
    )
  }

}
