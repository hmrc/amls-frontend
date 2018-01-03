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

package generators.businessmatching

import generators.BaseGenerator
import generators.businesscustomer.ReviewDetailsGenerator
import models.businessmatching.{BusinessMatching, BusinessType}
import org.scalacheck.Gen
import cats.implicits._
import models.businesscustomer.ReviewDetails


trait BusinessMatchingGenerator extends BaseGenerator
  with ReviewDetailsGenerator
  with BusinessActivitiesGenerator
  with PsrNumberGen {

  val businessMatchingGen: Gen[BusinessMatching] = for {
    reviewDetails <- reviewDetailsGen
    activities <- businessActivitiesGen
  } yield BusinessMatching(Some(reviewDetails), Some(activities))

  val businessMatchingWithPsrGen: Gen[BusinessMatching] = for {
    bm <- businessMatchingGen
    psr <- psrNumberGen
  } yield bm.copy(businessAppliedForPSRNumber = Some(psr))

  def businessMatchingWithTypesGen(bType: Option[BusinessType]) = for {
    bm <- businessMatchingGen
  } yield bm.copy(reviewDetails = bm.reviewDetails.fold(none[ReviewDetails])(rd => rd.copy(businessType = bType).some))

}
