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

import generators.{BaseGenerator, CountryGenerator}
import models.businesscustomer.Address
import org.scalacheck.Gen

trait AddressGenerator extends BaseGenerator with CountryGenerator {

  private val nameLength = 10

  val addressGen: Gen[Address] = for {
    line1 <- stringOfLengthGen(nameLength)
    line2 <- stringOfLengthGen(nameLength)
    postcode <- postcodeGen
    country <- countryGen
  } yield Address(line1, line2, None, None, Some(postcode), country)

}
