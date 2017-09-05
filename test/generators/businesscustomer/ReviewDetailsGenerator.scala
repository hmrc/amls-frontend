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

package generators.businesscustomer

import generators.businessmatching.BusinessTypeGenerator
import generators.{BaseGenerator, CountryGenerator}
import models.businesscustomer.ReviewDetails
import org.scalacheck.Gen

//noinspection ScalaStyle
trait ReviewDetailsGenerator extends BaseGenerator
  with AddressGenerator
  with CountryGenerator
  with BusinessTypeGenerator {

  val reviewDetailsGen: Gen[ReviewDetails] = for {
    businessName <- stringOfLengthGen(20)
    bType <- businessTypeGen
    address <- addressGen
    safeId <- safeIdGen
    utr <- numSequence(8)
  } yield ReviewDetails(businessName, Some(bType), address, safeId, Some(utr))

}
