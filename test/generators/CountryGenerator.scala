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

import models.Country
import org.scalacheck.Gen

trait CountryGenerator extends BaseGenerator {

  private val nameLength = 10

  val countryGen: Gen[Country] = for {
    name <- stringOfLengthGen(nameLength)
    charCode <- stringOfLengthGen(3)
  } yield Country(name, charCode.toUpperCase)

}
